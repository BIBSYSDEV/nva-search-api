package no.unit.nva.search.ticket;

import static no.unit.nva.search.ticket.Constants.ANY_OF_TICKET_TYPE_OR_USER_NAME;
import static no.unit.nva.search.ticket.Constants.NOT_ANY_OF_TICKET_TYPE;
import static no.unit.nva.search.ticket.Constants.OWNER_USERNAME;
import static no.unit.nva.search.ticket.Constants.TYPE_KEYWORD;
import static no.unit.nva.search.ticket.Constants.USER_IS_NOT_ALLOWED_TO_SEARCH_FOR_TICKETS_NOT_OWNED_BY_THEMSELVES;
import static no.unit.nva.search.ticket.TicketParameter.ORGANIZATION_ID;
import static no.unit.nva.search.ticket.TicketParameter.OWNER;
import static no.unit.nva.search.ticket.TicketParameter.STATISTICS;

import static nva.commons.apigateway.AccessRight.MANAGE_CUSTOMERS;
import static nva.commons.apigateway.AccessRight.MANAGE_DOI;
import static nva.commons.apigateway.AccessRight.MANAGE_PUBLISHING_REQUESTS;

import static org.opensearch.index.query.QueryBuilders.boolQuery;
import static org.opensearch.index.query.QueryBuilders.termsQuery;

import static java.util.Objects.isNull;

import no.unit.nva.search.common.builder.AcrossFieldsQuery;
import no.unit.nva.search.common.records.FilterBuilder;

import nva.commons.apigateway.AccessRight;
import nva.commons.apigateway.RequestInfo;
import nva.commons.apigateway.exceptions.UnauthorizedException;

import org.opensearch.index.query.BoolQueryBuilder;
import org.opensearch.index.query.DisMaxQueryBuilder;
import org.opensearch.index.query.QueryBuilder;
import org.opensearch.index.query.QueryBuilders;
import org.opensearch.index.query.TermQueryBuilder;
import org.opensearch.index.query.TermsQueryBuilder;

import java.net.URI;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * TicketAccessFilter is a class that filters tickets based on access rights.
 *
 * <ul>
 *   <lh>Business Rules</lh>
 *   <li>manage_doi -> DOI_REQUEST
 *   <li>support -> GENERAL_SUPPORT_CASE
 *   <li>manage_publishing_requests -> PUBLISHING_REQUEST
 *   <li>manage_customers -> DOI_REQUEST, GENERAL_SUPPORT_CASE, PUBLISHING_REQUEST
 *   <li>is_owner -> DOI_REQUEST, GENERAL_SUPPORT_CASE, PUBLISHING_REQUEST
 * </ul>
 *
 * @author Stig Norland
 * @author Sondre Vestad
 * @author Lars-Olav Vågene
 * @author Kir Truhacev
 */
public class TicketAccessFilter implements FilterBuilder<TicketSearchQuery> {

    private final TicketSearchQuery ticketSearchQuery;
    private String currentUser;
    private URI organizationId;
    private Set<AccessRight> accessRights = EnumSet.noneOf(AccessRight.class);
    private Set<TicketType> curatorTicketTypes;

    //    private Set<TicketType> excludeTicketTypes;

    public TicketAccessFilter(TicketSearchQuery ticketSearchQuery) {
        this.ticketSearchQuery = ticketSearchQuery;
        this.ticketSearchQuery.filters().set();
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

        if (searchAsSiktAdmin() && validateSiktAdmin(accessRights)) {
            return ticketSearchQuery; // See everything, NO FILTERS!!!
        }

        if (isNull(organizationId)) { // Default, see only tickets for this organization
            throw new UnauthorizedException("Organization is required");
        }
        this.ticketSearchQuery.filters().add(getOrgFilter());

        this.curatorTicketTypes = curatorsAllowedTicketTypes(accessRights);

        if (searchAsTicketOwner() && curatorTicketTypes.isEmpty()) {
            validateOwner(currentUser);
            // If the user is not a curator, they can only see their own tickets
        }

        this.ticketSearchQuery.filters().add(getMineAndCuratorFilter());

        return ticketSearchQuery;
    }

    /**
     * Filter on owner (user).
     *
     * @param userName current user
     * @return TicketQuery (builder pattern)
     * @apiNote ONLY SET THIS MANUALLY IN TESTS
     */
    public TicketAccessFilter user(String userName) {
        this.currentUser = userName;
        return this;
    }

    /**
     * Filter on organization.
     *
     * @param organization organization id
     * @return TicketQuery (builder pattern)
     * @apiNote ONLY SET THIS MANUALLY IN TESTS
     */
    public TicketAccessFilter organization(URI organization) {
        this.organizationId = organization;
        return this;
    }

    /**
     * Filter on access rights.
     *
     * @param accessRights access rights
     * @return TicketQuery (builder pattern)
     * @apiNote ONLY SET THIS MANUALLY IN TESTS
     */
    public TicketAccessFilter accessRights(AccessRight... accessRights) {
        return accessRights(List.of(accessRights));
    }

    private TicketAccessFilter accessRights(List<AccessRight> accessRights) {
        this.accessRights =
                accessRights.stream()
                        .collect(Collectors.toCollection(() -> EnumSet.noneOf(AccessRight.class)));
        return this;
    }

    public String getCurrentUser() {
        return currentUser;
    }

    protected DisMaxQueryBuilder getMineAndCuratorFilter() {
        return QueryBuilders.disMaxQuery()
                .add(new TermQueryBuilder(OWNER_USERNAME, currentUser))
                .add(new TermsQueryBuilder(TYPE_KEYWORD, curatorTicketTypes))
                .add(getExcludedFilter())
                .queryName(ANY_OF_TICKET_TYPE_OR_USER_NAME);
    }

    protected QueryBuilder getOrgFilter() {
        return new AcrossFieldsQuery<TicketParameter>()
                .buildQuery(ORGANIZATION_ID, organizationId.toString())
                .findFirst()
                .orElseThrow()
                .getValue()
                .queryName(ORGANIZATION_ID.asCamelCase());
    }

    protected BoolQueryBuilder getExcludedFilter() {
        var excludedSet =
                Arrays.stream(TicketType.values())
                        .filter(f -> !curatorTicketTypes.contains(f))
                        .collect(Collectors.toCollection(() -> EnumSet.noneOf(TicketType.class)));
        return boolQuery()
                .mustNot(termsQuery(TYPE_KEYWORD, excludedSet))
                .queryName(NOT_ANY_OF_TICKET_TYPE);
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
    private Set<TicketType> curatorsAllowedTicketTypes(Set<AccessRight> accessRights) {
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
        return allowed;
    }

    //    private Set<TicketType> listToEnumSet(TicketType... ticketTypes) {
    //        return Arrays.stream(ticketTypes)
    //                .collect(Collectors.toCollection(() -> EnumSet.noneOf(TicketType.class)));
    //    }

    private boolean validateSiktAdmin(Set<AccessRight> accessRights) throws UnauthorizedException {
        if (!accessRights.contains(MANAGE_CUSTOMERS)) {
            throw new UnauthorizedException(
                    USER_IS_NOT_ALLOWED_TO_SEARCH_FOR_TICKETS_NOT_OWNED_BY_THEMSELVES);
        }
        return true;
    }

    private void validateOwner(String userName) throws UnauthorizedException {
        if (currentUserIsNotOwner(userName)) {
            throw new UnauthorizedException(
                    USER_IS_NOT_ALLOWED_TO_SEARCH_FOR_TICKETS_NOT_OWNED_BY_THEMSELVES);
        }
    }

    private boolean currentUserIsNotOwner(String userName) {
        return !userName.equalsIgnoreCase(ticketSearchQuery.parameters().get(OWNER).as());
    }

    private boolean searchAsTicketOwner() {
        return ticketSearchQuery.parameters().isPresent(OWNER);
    }

    private boolean searchAsSiktAdmin() {
        return ticketSearchQuery.parameters().isPresent(STATISTICS);
    }
}
