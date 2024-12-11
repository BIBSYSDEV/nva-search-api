package no.unit.nva.search.ticket;

import static no.unit.nva.search.ticket.Constants.CANNOT_SEARCH_AS_BOTH_ASSIGNEE_AND_OWNER_AT_THE_SAME_TIME;
import static no.unit.nva.search.ticket.Constants.FILTER_BY_ORGANIZATION;
import static no.unit.nva.search.ticket.Constants.FILTER_BY_OWNER;
import static no.unit.nva.search.ticket.Constants.FILTER_BY_TICKET_TYPES;
import static no.unit.nva.search.ticket.Constants.FILTER_BY_UN_PUBLISHED;
import static no.unit.nva.search.ticket.Constants.FILTER_BY_USER_AND_TICKET_TYPES;
import static no.unit.nva.search.ticket.Constants.ORGANIZATION_IS_REQUIRED;
import static no.unit.nva.search.ticket.Constants.ORGANIZATION_PATHS;
import static no.unit.nva.search.ticket.Constants.TYPE_KEYWORD;
import static no.unit.nva.search.ticket.Constants.USER_IS_NOT_ALLOWED_TO_SEARCH_FOR_TICKETS_NOT_OWNED_BY_THEMSELVES;
import static no.unit.nva.search.ticket.Constants.USER_IS_REQUIRED;
import static no.unit.nva.search.ticket.TicketParameter.ASSIGNEE;
import static no.unit.nva.search.ticket.TicketParameter.OWNER;
import static no.unit.nva.search.ticket.TicketParameter.STATISTICS;

import static nva.commons.apigateway.AccessRight.MANAGE_CUSTOMERS;
import static nva.commons.apigateway.AccessRight.MANAGE_DOI;
import static nva.commons.apigateway.AccessRight.MANAGE_PUBLISHING_REQUESTS;

import static org.opensearch.index.query.QueryBuilders.boolQuery;

import static java.util.Objects.isNull;

import no.unit.nva.search.common.records.FilterBuilder;

import nva.commons.apigateway.AccessRight;
import nva.commons.apigateway.RequestInfo;
import nva.commons.apigateway.exceptions.UnauthorizedException;

import org.opensearch.index.query.BoolQueryBuilder;
import org.opensearch.index.query.MultiMatchQueryBuilder;
import org.opensearch.index.query.Operator;
import org.opensearch.index.query.QueryBuilder;
import org.opensearch.index.query.QueryBuilders;
import org.opensearch.index.query.TermsQueryBuilder;

import java.net.URI;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * TicketAccessFilter is a class that filters tickets based on access rights.
 *
 * <ul>
 *   <lh>Business Rules</lh>
 *   <li>MANAGE_DOI -> DoiRequest
 *   <li>SUPPORT -> GeneralSupportCase
 *   <li>MANAGE_PUBLISHING_REQUESTS -> PublishingRequest
 *   <li>MANAGE_CUSTOMERS -> DoiRequest, GeneralSupportCase, PublishingRequest
 *   <li>IS_OWNER -> ignore ticket TYPE, only show tickets owned by the user
 *   <li>IS_ASSIGNEE -> ignore OWNER, only show ticket types that user has access to
 * </ul>
 *
 * @author Stig Norland
 * @author Sondre Vestad
 * @author Lars-Olav Vågene
 * @author Kir Truhacev
 */
public class TicketAccessFilter implements FilterBuilder<TicketSearchQuery> {

    private final TicketSearchQuery query;
    private String currentUser;
    private URI organizationId;
    private Set<AccessRight> accessRightEnumSet = EnumSet.noneOf(AccessRight.class);

    public TicketAccessFilter(TicketSearchQuery query) {
        this.query = query;
        this.query.filters().set();
    }

    public String getCurrentUser() {
        return currentUser;
    }

    /**
     * Filter on access rights.
     *
     * @param accessRights access rights
     * @return TicketQuery (builder pattern)
     * @apiNote ONLY USE IN TESTS, in handlers use: {@link #fromRequestInfo(RequestInfo)}
     */
    public TicketAccessFilter accessRights(AccessRight... accessRights) {
        return accessRights(List.of(accessRights));
    }

    private TicketAccessFilter accessRights(List<AccessRight> accessRights) {
        this.accessRightEnumSet =
                accessRights.stream()
                        .collect(Collectors.toCollection(() -> EnumSet.noneOf(AccessRight.class)));
        return this;
    }

    /**
     * Filter on owner (user).
     *
     * @param userName current user
     * @return TicketQuery (builder pattern)
     * @apiNote ONLY USE IN TESTS, in handlers use: {@link #fromRequestInfo(RequestInfo)}
     */
    public TicketAccessFilter user(String userName) {
        this.currentUser = userName;
        return this;
    }

    /**
     * Filter on organization.
     *
     * @param organization organization id
     * @apiNote ONLY USE IN TESTS, in handlers use: {@link #fromRequestInfo(RequestInfo)}
     */
    public TicketAccessFilter organization(URI organization) {
        this.organizationId = organization;
        return this;
    }

    /**
     * Authorize and set 'ViewScope'.
     *
     * <p>Authorize and set filters -> ticketTypes, organization and owner
     *
     * <p>This is to avoid the Query to return documents that are not available for the user.
     *
     * @param requestInfo all required is here
     * @return TicketQuery (builder pattern)
     */
    @Override
    public TicketSearchQuery fromRequestInfo(RequestInfo requestInfo) throws UnauthorizedException {

        var organization =
                requestInfo.getTopLevelOrgCristinId().orElse(requestInfo.getPersonAffiliation());

        return user(requestInfo.getUserName())
                .accessRights(requestInfo.getAccessRights())
                .organization(organization)
                .apply();
    }

    @Override
    public TicketSearchQuery apply() throws UnauthorizedException {

        if (searchAsSiktAdmin() && validateSiktAdmin(accessRightEnumSet)) {
            return query; // See everything, NO FILTERS!!!
        }

        validateOrganization();
        validateUser();
        final var curatorTicketTypes = accessRightsToTicketTypes(accessRightEnumSet);

        if (hasNoCuratorRoles(curatorTicketTypes) && searchAsTicketOwner()) {
            validateOwner(currentUser);
        }
        if (searchAsTicketAssignee() && searchAsTicketOwner()) {
            validateAssigneeAndOwnerParameters();
        }

        this.query.filters().add(filterByOrganization(organizationId));
        this.query.filters().add(filterByUserAndTicketTypes(currentUser, curatorTicketTypes));
        this.query.filters().add(filterByUnPublished());
        return query;
    }

    /**
     * Apply business rules to determine which ticket types are allowed.
     *
     * <ul>
     *   <li>manage_doi -> DOI_REQUEST
     *   <li>support -> GENERAL_SUPPORT_CASE
     *   <li>manage_publishing_requests -> PUBLISHING_REQUEST
     * </ul>
     */
    private Set<TicketType> accessRightsToTicketTypes(Set<AccessRight> accessRights) {
        var allowed = EnumSet.noneOf(TicketType.class);
        if (accessRights.contains(MANAGE_DOI)) {
            allowed.add(TicketType.DOI_REQUEST);
        }
        if (accessRights.contains(AccessRight.SUPPORT)) {
            allowed.add(TicketType.GENERAL_SUPPORT_CASE);
        }
        if (accessRights.contains(MANAGE_PUBLISHING_REQUESTS)) {
            allowed.add(TicketType.PUBLISHING_REQUEST);
        }
        if (allowed.isEmpty()) {
            allowed.add(TicketType.NONE);
        }
        return allowed;
    }

    private QueryBuilder filterByOrganization(URI organizationId) {
        return QueryBuilders.multiMatchQuery(organizationId.toString(), ORGANIZATION_PATHS)
                .type(MultiMatchQueryBuilder.Type.CROSS_FIELDS)
                .operator(Operator.AND)
                .queryName(FILTER_BY_ORGANIZATION);
    }

    private BoolQueryBuilder filterByUserAndTicketTypes(
            String userName, Set<TicketType> curatorTicketTypes) {
        return boolQuery()
                .should(filterByOwner(userName))
                .should(filterByTicketTypes(curatorTicketTypes))
                .minimumShouldMatch(1)
                .queryName(FILTER_BY_USER_AND_TICKET_TYPES);
    }

    private QueryBuilder filterByOwner(String userName) {
        return QueryBuilders.multiMatchQuery(userName, OWNER.searchFields().toArray(String[]::new))
                .type(MultiMatchQueryBuilder.Type.CROSS_FIELDS)
                .operator(Operator.AND)
                .queryName(FILTER_BY_OWNER);
    }

    private QueryBuilder filterByTicketTypes(Set<TicketType> curatorTicketTypes) {
        var ticketTypes =
                curatorTicketTypes.stream().map(TicketType::toString).toArray(String[]::new);
        return new TermsQueryBuilder(TYPE_KEYWORD, ticketTypes).queryName(FILTER_BY_TICKET_TYPES);
    }

    private QueryBuilder filterByUnPublished() {
        return boolQuery().mustNot(filterByTicketTypes(Set.of(TicketType.UNPUBLISH_REQUEST)))
            .queryName(FILTER_BY_UN_PUBLISHED);
    }

    private void validateOrganization() throws UnauthorizedException {
        if (isNull(organizationId)) {
            throw new UnauthorizedException(ORGANIZATION_IS_REQUIRED);
        }
    }

    private void validateOwner(String userName) throws UnauthorizedException {
        if (isCurrentUserNotOwner(userName)) {
            throw new UnauthorizedException(
                    USER_IS_NOT_ALLOWED_TO_SEARCH_FOR_TICKETS_NOT_OWNED_BY_THEMSELVES);
        }
    }

    private void validateAssigneeAndOwnerParameters() throws UnauthorizedException {
        if (query.parameters().get(OWNER).as().equals(query.parameters().get(ASSIGNEE).as())) {
            throw new UnauthorizedException(
                    CANNOT_SEARCH_AS_BOTH_ASSIGNEE_AND_OWNER_AT_THE_SAME_TIME);
        }
    }

    private void validateUser() throws UnauthorizedException {
        if (isNull(currentUser)) {
            throw new UnauthorizedException(USER_IS_REQUIRED);
        }
    }

    private boolean validateSiktAdmin(Set<AccessRight> accessRights) throws UnauthorizedException {
        if (!accessRights.contains(MANAGE_CUSTOMERS)) {
            throw new UnauthorizedException(
                    USER_IS_NOT_ALLOWED_TO_SEARCH_FOR_TICKETS_NOT_OWNED_BY_THEMSELVES);
        }
        return true;
    }

    private boolean searchAsTicketAssignee() {
        return query.parameters().isPresent(ASSIGNEE);
    }

    private boolean hasNoCuratorRoles(Set<TicketType> curatorTicketTypes) {
        return curatorTicketTypes.contains(TicketType.NONE);
    }

    private boolean isCurrentUserNotOwner(String userName) {
        return !userName.equalsIgnoreCase(query.parameters().get(OWNER).as());
    }

    private boolean searchAsTicketOwner() {
        return query.parameters().isPresent(OWNER);
    }

    private boolean searchAsSiktAdmin() {
        return query.parameters().isPresent(STATISTICS);
    }
}
