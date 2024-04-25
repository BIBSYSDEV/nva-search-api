package no.unit.nva.search2.ticket;

import static java.util.Objects.nonNull;
import no.unit.nva.search2.common.builder.OpensearchQueryKeyword;
import static no.unit.nva.search2.common.constant.Words.POST_FILTER;
import static no.unit.nva.search2.ticket.Constants.OWNER_USERNAME;
import static no.unit.nva.search2.ticket.Constants.TYPE_KEYWORD;
import static no.unit.nva.search2.ticket.TicketParameter.ORGANIZATION_ID;
import nva.commons.apigateway.AccessRight;
import static nva.commons.apigateway.AccessRight.MANAGE_DOI;
import static nva.commons.apigateway.AccessRight.MANAGE_PUBLISHING_REQUESTS;
import nva.commons.apigateway.RequestInfo;
import nva.commons.apigateway.exceptions.UnauthorizedException;
import org.opensearch.index.query.QueryBuilders;
import org.opensearch.index.query.TermQueryBuilder;
import org.opensearch.index.query.TermsQueryBuilder;

import java.net.URI;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

public class TicketFilter {

    private final TicketQuery ticketQuery;
    private String currentUser;
    private List<String> ticketTypes;

    public TicketFilter(TicketQuery ticketQuery) {
        Objects.requireNonNull(ticketQuery);
        this.ticketQuery = ticketQuery;
    }

    public String getCurrentUser() {
        return currentUser;
    }

    /**
     * Authorize and set 'ViewScope'.
     *
     * <p>Authorize and set filters -> ticketTypes, organization & owner</p>
     * <p>This is to avoid the Query to return documents that are not available for the user.</p>
     *
     * @param requestInfo all required is here
     * @return TicketQuery (builder pattern)
     */
    public TicketQuery applyContextAndAuthorize(RequestInfo requestInfo) throws UnauthorizedException {
        if (Objects.isNull(requestInfo.getUserName())) {
            throw new UnauthorizedException();
        }

        final var organization = requestInfo
            .getTopLevelOrgCristinId()
            .orElse(requestInfo.getPersonAffiliation());

        final var curatorRights = getAccessRights(requestInfo.getAccessRights())
            .toArray(TicketType[]::new);

        return withFilterOrganization(organization)
            .withTicketType(curatorRights)
            .withCurrentUser(requestInfo.getUserName())
            .applyFilters();
    }

    /**
     * Filter on owner (user).
     *
     * <P>ONLY SET THIS MANUALLY IN TESTS</P>
     * <p>Only tickets owned by user will be available for the Query.</p>
     * <p>This is to avoid the Query to return documents that are not available for the user.</p>
     *
     * @param userName current user
     * @return TicketQuery (builder pattern)
     */
    public TicketFilter withCurrentUser(String userName) {
        this.currentUser = userName;
        return this;
    }

    /**
     * Applies user and type filters
     *
     * <P>ONLY SET THIS MANUALLY IN TESTS</P>
     *
     * @return ResourceQuery (builder pattern)
     */
    public TicketQuery applyFilters() {
        var disMax = QueryBuilders
            .disMaxQuery()
            .queryName("anyOfTicketTypeUserName")
            .add(new TermQueryBuilder(OWNER_USERNAME, currentUser));
        if (nonNull(ticketTypes)) {
            disMax.add(new TermsQueryBuilder(TYPE_KEYWORD, ticketTypes));
        }
        this.ticketQuery.filters.add(disMax);
        return ticketQuery;
    }

    /**
     * Filter on organization.
     * <P>ONLY SET THIS MANUALLY IN TESTS</P>
     * <P>Only documents belonging to organization specified are searchable (for the user)
     * </p>
     *
     * @param organization uri of publisher
     * @return ResourceQuery (builder pattern)
     */
    public TicketFilter withFilterOrganization(URI organization) {
        final var filter =
            new OpensearchQueryKeyword<TicketParameter>()
                .buildQuery(ORGANIZATION_ID, organization.toString())
                .findFirst().orElseThrow().getValue()
                .queryName(ORGANIZATION_ID.asCamelCase() + POST_FILTER);
        this.ticketQuery.filters.add(filter);
        return this;
    }

    /**
     * Filter on Required Types.
     *
     * <P>ONLY SET THIS MANUALLY IN TESTS</P>
     * <p>Only TYPE specified here will be available for the Query.</p>
     * <p>This is to avoid the Query to return documents that are not available for the user.</p>
     * <p>See {@link TicketType} for available values.</p>
     *
     * @param ticketTypes the required types
     * @return TicketQuery (builder pattern)
     */
    public TicketFilter withTicketType(TicketType... ticketTypes) {
        this.ticketTypes = Arrays.stream(ticketTypes).map(TicketType::toString).toList();
        return this;
    }


    private Set<TicketType> getAccessRights(List<AccessRight> accessRights) {
        var allowed = new HashSet<TicketType>();
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
