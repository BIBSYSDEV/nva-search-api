package no.unit.nva.search.ticket;

import static no.unit.nva.constants.Words.POST_FILTER;
import static no.unit.nva.search.ticket.Constants.OWNER_USERNAME;
import static no.unit.nva.search.ticket.Constants.TYPE_KEYWORD;
import static no.unit.nva.search.ticket.TicketParameter.ORGANIZATION_ID;
import static no.unit.nva.search.ticket.TicketParameter.STATISTICS;

import static nva.commons.apigateway.AccessRight.MANAGE_DOI;
import static nva.commons.apigateway.AccessRight.MANAGE_PUBLISHING_REQUESTS;

import no.unit.nva.search.common.builder.KeywordQuery;
import no.unit.nva.search.common.records.FilterBuilder;

import nva.commons.apigateway.AccessRight;
import nva.commons.apigateway.RequestInfo;
import nva.commons.apigateway.exceptions.UnauthorizedException;

import org.opensearch.index.query.QueryBuilders;
import org.opensearch.index.query.TermQueryBuilder;
import org.opensearch.index.query.TermsQueryBuilder;

import java.net.URI;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

/**
 * @author Stig Norland
 */
public class TicketFilter implements FilterBuilder<TicketSearchQuery> {

    private final TicketSearchQuery ticketSearchQuery;
    private String currentUser;

    public TicketFilter(TicketSearchQuery ticketSearchQuery) {
        this.ticketSearchQuery = ticketSearchQuery;
        this.ticketSearchQuery.filters.set();
    }

    @Override
    public TicketSearchQuery apply() {
        return ticketSearchQuery;
    }

    /**
     * Authorize and set 'ViewScope'.
     *
     * <p>Authorize and set filters -> ticketTypes, organization & owner
     *
     * <p>This is to avoid the Query to return documents that are not available for the user.
     *
     * @param requestInfo all required is here
     * @return TicketQuery (builder pattern)
     */
    @Override
    public TicketSearchQuery fromRequestInfo(RequestInfo requestInfo) throws UnauthorizedException {
        if (Objects.isNull(requestInfo.getUserName())) {
            throw new UnauthorizedException();
        }

        if (isSearchingForAllTickets(requestInfo)) {
            return ticketSearchQuery;
        }

        final var organization =
                requestInfo.getTopLevelOrgCristinId().orElse(requestInfo.getPersonAffiliation());

        final var curatorRights =
                getAccessRights(requestInfo.getAccessRights()).toArray(TicketType[]::new);

        return organization(organization)
                .userAndTicketTypes(requestInfo.getUserName(), curatorRights)
                .apply();
    }

    private boolean isSearchingForAllTickets(RequestInfo requestInfo) {
        return ticketSearchQuery.parameters().isPresent(STATISTICS)
                && requestInfo.userIsAuthorized(AccessRight.MANAGE_CUSTOMERS);
    }

    /**
     * Filter on owner (user).
     *
     * <p>ONLY SET THIS MANUALLY IN TESTS
     *
     * <p>Only tickets owned by user will be available for the Query.
     *
     * <p>This is to avoid the Query to return documents that are not available for the user.
     *
     * @param userName current user
     * @return TicketQuery (builder pattern)
     */
    public TicketFilter userAndTicketTypes(String userName, TicketType... ticketTypes) {
        this.currentUser = userName;
        var ticketTypeList = Arrays.stream(ticketTypes).map(TicketType::toString).toList();
        var disMax =
                QueryBuilders.disMaxQuery()
                        .queryName("anyOfTicketTypeUserName")
                        .add(new TermQueryBuilder(OWNER_USERNAME, currentUser));
        if (!ticketTypeList.isEmpty()) {
            disMax.add(new TermsQueryBuilder(TYPE_KEYWORD, ticketTypeList));
        }
        this.ticketSearchQuery.filters.add(disMax);
        return this;
    }

    /**
     * Filter on organization.
     *
     * <p>ONLY SET THIS MANUALLY IN TESTS
     *
     * <p>Only documents belonging to organization specified are searchable (for the user)
     *
     * @param organization uri of publisher
     * @return ResourceQuery (builder pattern)
     */
    public TicketFilter organization(URI organization) {
        final var organisationId =
                new KeywordQuery<TicketParameter>()
                        .buildQuery(ORGANIZATION_ID, organization.toString())
                        .findFirst()
                        .orElseThrow()
                        .getValue()
                        .queryName(ORGANIZATION_ID.asCamelCase() + POST_FILTER);
        this.ticketSearchQuery.filters.add(organisationId);
        return this;
    }

    public String getCurrentUser() {
        return currentUser;
    }

    private Set<TicketType> getAccessRights(List<AccessRight> accessRights) {

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
}
