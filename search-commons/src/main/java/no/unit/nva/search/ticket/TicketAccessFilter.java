package no.unit.nva.search.ticket;

import static no.unit.nva.search.ticket.Constants.OWNER_USERNAME;
import static no.unit.nva.search.ticket.Constants.TYPE_KEYWORD;
import static no.unit.nva.search.ticket.TicketParameter.ORGANIZATION_ID;
import static no.unit.nva.search.ticket.TicketParameter.OWNER;
import static no.unit.nva.search.ticket.TicketParameter.STATISTICS;
import static no.unit.nva.search.ticket.TicketType.UNPUBLISH_REQUEST;

import static nva.commons.apigateway.AccessRight.MANAGE_CUSTOMERS;
import static nva.commons.apigateway.AccessRight.MANAGE_DOI;
import static nva.commons.apigateway.AccessRight.MANAGE_PUBLISHING_REQUESTS;

import static org.opensearch.index.query.QueryBuilders.boolQuery;
import static org.opensearch.index.query.QueryBuilders.termsQuery;

import static java.util.Objects.nonNull;

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

    private static final String NOT_ANY_OF_TICKET_TYPE = "notAnyOfTicketType";
    private static final String ANY_OF_TICKET_TYPE_OR_USER_NAME = "anyOfTicketTypeOrUserName";
    private static final String USER_HAS_NO_ACCESS_TO_SEARCH_FOR_ANY_TICKET_TYPES =
            "User has no access to search for any ticket types!";
    private static final String USER_IS_NOT_ALLOWED_TO_SEARCH_FOR_TICKETS_NOT_OWNED_BY_THEMSELVES =
            "User is not allowed to search for tickets not owned by themselves";

    private final TicketSearchQuery ticketSearchQuery;
    private String currentUser;
    private Set<TicketType> allowedTicketTypes;
    private Set<TicketType> excludeTicketTypes;
    private URI organizationId;

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
        currentUser = requestInfo.getUserName();

        final var organization =
                requestInfo.getTopLevelOrgCristinId().orElse(requestInfo.getPersonAffiliation());

        final var ticketTypes = applyBusinessRules(requestInfo.getAccessRights());

        return organization(organization)
                .includedTypes(ticketTypes)
                .excludeTypes(UNPUBLISH_REQUEST)
                .apply();
    }

    @Override
    public TicketSearchQuery apply() {
        if (nonNull(organizationId)) {
            this.ticketSearchQuery.filters().add(getOrgFilter());
        }

        if (nonNull(currentUser) && nonNull(allowedTicketTypes)) {
            this.ticketSearchQuery.filters().add(getTicketTypeUserNameFilter());
        }

        if (nonNull(excludeTicketTypes) || nonNull(allowedTicketTypes)) {
            this.ticketSearchQuery.filters().add(getExcludedFilter());
        }

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
     * Filter on ticket types.
     *
     * @param ticketTypes ticket types available for the user
     * @return TicketQuery (builder pattern)
     * @apiNote ONLY SET THIS MANUALLY IN TESTS
     */
    public TicketAccessFilter includedTypes(TicketType... ticketTypes) {
        return includedTypes(listToEnumSet(ticketTypes));
    }

    /**
     * Exclude these ticket types.
     *
     * @param ticketTypes ticket types to exclude
     * @return TicketQuery (builder pattern)
     * @apiNote ONLY SET THIS MANUALLY IN TESTS
     */
    public TicketAccessFilter excludeTypes(TicketType... ticketTypes) {
        return excludeTypes(listToEnumSet(ticketTypes));
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

    public String getCurrentUser() {
        return currentUser;
    }

    protected DisMaxQueryBuilder getTicketTypeUserNameFilter() {
        return QueryBuilders.disMaxQuery()
                .add(new TermQueryBuilder(OWNER_USERNAME, currentUser))
                .add(new TermsQueryBuilder(TYPE_KEYWORD, allowedTicketTypes))
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
                nonNull(excludeTicketTypes)
                        ? excludeTicketTypes
                        : Arrays.stream(TicketType.values())
                                .filter(f -> !allowedTicketTypes.contains(f))
                                .collect(
                                        Collectors.toCollection(
                                                () -> EnumSet.noneOf(TicketType.class)));
        return boolQuery()
                .mustNot(termsQuery(TYPE_KEYWORD, excludedSet))
                .queryName(NOT_ANY_OF_TICKET_TYPE);
    }

    private TicketAccessFilter includedTypes(Set<TicketType> ticketTypes) {
        this.allowedTicketTypes = ticketTypes;
        return this;
    }

    private TicketAccessFilter excludeTypes(Set<TicketType> ticketTypes) {
        this.excludeTicketTypes = ticketTypes;
        return this;
    }

    /**
     * Apply business rules to determine which ticket types are allowed.
     *
     * <ul>
     *   <lh>Business Rules.</lh>
     *   <li>manage_doi -> DOI_REQUEST
     *   <li>support -> GENERAL_SUPPORT_CASE
     *   <li>manage_publishing_requests -> PUBLISHING_REQUEST
     *   <li>manage_customers -> DOI_REQUEST, GENERAL_SUPPORT_CASE, PUBLISHING_REQUEST
     *   <li>is_owner -> DOI_REQUEST, GENERAL_SUPPORT_CASE, PUBLISHING_REQUEST
     * </ul>
     */
    private Set<TicketType> applyBusinessRules(List<AccessRight> accessRights)
            throws UnauthorizedException {

        var isOwner = searchAsTicketOwner() && validateOwner(currentUser);
        var isCustomManager = searchAsCustomManager() && validateCustomManager(accessRights);

        var allowed = EnumSet.noneOf(TicketType.class);
        if (accessRights.contains(MANAGE_DOI) || isOwner || isCustomManager) {
            allowed.add(TicketType.DOI_REQUEST);
        }
        if (accessRights.contains(AccessRight.SUPPORT) || isOwner || isCustomManager) {
            allowed.add(TicketType.GENERAL_SUPPORT_CASE);
        }
        if (accessRights.contains(MANAGE_PUBLISHING_REQUESTS) || isOwner || isCustomManager) {
            allowed.add(TicketType.PUBLISHING_REQUEST);
        }
        if (allowed.isEmpty()) {
            throw new UnauthorizedException(USER_HAS_NO_ACCESS_TO_SEARCH_FOR_ANY_TICKET_TYPES);
        }
        return allowed;
    }

    private Set<TicketType> listToEnumSet(TicketType... ticketTypes) {
        return Arrays.stream(ticketTypes)
                .collect(Collectors.toCollection(() -> EnumSet.noneOf(TicketType.class)));
    }

    private boolean validateCustomManager(List<AccessRight> accessRights)
            throws UnauthorizedException {
        if (!accessRights.contains(MANAGE_CUSTOMERS)) {
            throw new UnauthorizedException(
                    USER_IS_NOT_ALLOWED_TO_SEARCH_FOR_TICKETS_NOT_OWNED_BY_THEMSELVES);
        }
        return true;
    }

    private boolean validateOwner(String userName) throws UnauthorizedException {
        if (currentUserIsNotOwner(userName)) {
            throw new UnauthorizedException(
                    USER_IS_NOT_ALLOWED_TO_SEARCH_FOR_TICKETS_NOT_OWNED_BY_THEMSELVES);
        }
        return true;
    }

    private boolean currentUserIsNotOwner(String userName) {
        return !userName.equalsIgnoreCase(ticketSearchQuery.parameters().get(OWNER).as());
    }

    private boolean searchAsTicketOwner() {
        return ticketSearchQuery.parameters().isPresent(OWNER);
    }

    private boolean searchAsCustomManager() {
        return ticketSearchQuery.parameters().isPresent(STATISTICS);
    }
}
