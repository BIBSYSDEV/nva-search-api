package no.unit.nva.search2.ticket;

import static no.unit.nva.search2.common.QueryTools.decodeUTF;
import static no.unit.nva.search2.common.constant.Defaults.DEFAULT_OFFSET;
import static no.unit.nva.search2.common.constant.Defaults.DEFAULT_SORT_ORDER;
import static no.unit.nva.search2.common.constant.Defaults.DEFAULT_VALUE_PER_PAGE;
import static no.unit.nva.search2.common.constant.ErrorMessages.INVALID_VALUE_WITH_SORT;
import static no.unit.nva.search2.common.constant.Words.ALL;
import static no.unit.nva.search2.common.constant.Words.COLON;
import static no.unit.nva.search2.common.constant.Words.COMMA;
import static no.unit.nva.search2.common.constant.Words.ID;
import static no.unit.nva.search2.common.constant.Words.NONE;
import static no.unit.nva.search2.common.constant.Words.POST_FILTER;
import static no.unit.nva.search2.common.constant.Words.SEARCH;
import static no.unit.nva.search2.common.constant.Words.TICKETS;
import static no.unit.nva.search2.common.constant.Words.TYPE;
import static no.unit.nva.search2.common.enums.TicketStatus.PENDING;
import static no.unit.nva.search2.ticket.Constants.DEFAULT_TICKET_SORT;
import static no.unit.nva.search2.ticket.Constants.ORGANIZATION;
import static no.unit.nva.search2.ticket.Constants.ORGANIZATION_ID_KEYWORD;
import static no.unit.nva.search2.ticket.Constants.OWNER_USERNAME;
import static no.unit.nva.search2.ticket.Constants.TYPE_KEYWORD;
import static no.unit.nva.search2.ticket.Constants.facetTicketsPaths;
import static no.unit.nva.search2.ticket.Constants.getTicketsAggregations;
import static no.unit.nva.search2.ticket.TicketParameter.AGGREGATION;
import static no.unit.nva.search2.ticket.TicketParameter.BY_USER_PENDING;
import static no.unit.nva.search2.ticket.TicketParameter.FIELDS;
import static no.unit.nva.search2.ticket.TicketParameter.FROM;
import static no.unit.nva.search2.ticket.TicketParameter.OWNER;
import static no.unit.nva.search2.ticket.TicketParameter.PAGE;
import static no.unit.nva.search2.ticket.TicketParameter.SEARCH_AFTER;
import static no.unit.nva.search2.ticket.TicketParameter.SIZE;
import static no.unit.nva.search2.ticket.TicketParameter.SORT;
import static no.unit.nva.search2.ticket.TicketParameter.SORT_ORDER;
import static no.unit.nva.search2.ticket.TicketParameter.STATUS;
import static no.unit.nva.search2.ticket.TicketParameter.TICKET_PARAMETER_SET;
import static no.unit.nva.search2.ticket.TicketSort.INVALID;
import static no.unit.nva.search2.ticket.TicketSort.fromSortKey;
import static no.unit.nva.search2.ticket.TicketSort.validSortKeys;
import static nva.commons.apigateway.AccessRight.MANAGE_DOI;
import static nva.commons.apigateway.AccessRight.MANAGE_PUBLISHING_REQUESTS;
import static nva.commons.core.attempt.Try.attempt;
import static nva.commons.core.paths.UriWrapper.fromUri;
import java.net.URI;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import java.util.UUID;
import java.util.stream.Stream;
import no.unit.nva.search2.common.AsType;
import no.unit.nva.search2.common.ParameterValidator;
import no.unit.nva.search2.common.Query;
import no.unit.nva.search2.common.builder.OpensearchQueryText;
import no.unit.nva.search2.common.enums.ValueEncoding;
import nva.commons.apigateway.AccessRight;
import nva.commons.apigateway.RequestInfo;
import nva.commons.apigateway.exceptions.UnauthorizedException;
import nva.commons.core.JacocoGenerated;
import org.opensearch.index.query.QueryBuilder;
import org.opensearch.index.query.TermQueryBuilder;
import org.opensearch.index.query.TermsQueryBuilder;
import org.opensearch.search.aggregations.AggregationBuilders;
import org.opensearch.search.aggregations.bucket.filter.FilterAggregationBuilder;
import org.opensearch.search.sort.SortOrder;

public final class TicketQuery extends Query<TicketParameter> {

    private String currentUser;
    private TicketType[] ticketTypes;

    private TicketQuery() {
        super();
        assignImpossibleWhiteListFilters();
    }

    /**
     * Add a (default) filter to the query that will never match any document.
     *
     * <p>This whitelist the Query from any forgetful developer (me)</p>
     * <p>i.e.In order to return any results, withFilter* must be set </p>
     */
    private void assignImpossibleWhiteListFilters() {
        var filterType =
            new TermsQueryBuilder(TYPE_KEYWORD, TicketType.NONE).queryName(TYPE + POST_FILTER);
        final var filterId =
            new TermQueryBuilder(ORGANIZATION_ID_KEYWORD, UUID.randomUUID()).queryName(ORGANIZATION + ID);
        filters.set(filterType, filterId);
    }

    public static TicketParameterValidator builder() {
        return new TicketParameterValidator();
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
        var organization = requestInfo.getTopLevelOrgCristinId()
            .orElse(requestInfo.getPersonAffiliation());

        return withFilterTicketType(validateAccessRight(requestInfo))
            .withFilterOrganization(organization)
            .withFilterCurrentUser(requestInfo.getUserName());
    }

    /**
     * Filter on owner (user).
     *
     * <p>Only tickets owned by user will be available for the Query.</p>
     * <p>This is to avoid the Query to return documents that are not available for the user.</p>
     *
     * @param userName current user
     * @return TicketQuery (builder pattern)
     */
    public TicketQuery withFilterCurrentUser(String userName) {
        this.currentUser = userName;
        if (isUserOnly(ticketTypes)) {
            final var viewOwnerOnly = new TermQueryBuilder(OWNER_USERNAME, userName)
                    .queryName(OWNER.asCamelCase());
            this.filters.add(viewOwnerOnly);
        }
        return this;
    }

    /**
     * Filter on organization.
     * <P>Only documents belonging to organization specified are searchable (for the user)
     * </p>
     *
     * @param organization uri of publisher
     * @return ResourceQuery (builder pattern)
     */
    public TicketQuery withFilterOrganization(URI organization) {
        final var filter = new TermQueryBuilder(ORGANIZATION_ID_KEYWORD, organization.toString())
            .queryName(ORGANIZATION + ID);
        this.filters.add(filter);
        return this;
    }

    /**
     * Filter on Required Types.
     *
     * <p>Only TYPE specified here will be available for the Query.</p>
     * <p>This is to avoid the Query to return documents that are not available for the user.</p>
     * <p>See {@link TicketType} for available values.</p>
     *
     * @param ticketTypes the required types
     * @return TicketQuery (builder pattern)
     */
    public TicketQuery withFilterTicketType(TicketType... ticketTypes) {
        this.ticketTypes = ticketTypes.clone();
        final var filter =
            new TermsQueryBuilder(TYPE_KEYWORD, Arrays.stream(ticketTypes).map(TicketType::toString).toList())
                .queryName(TICKETS + TYPE);
        this.filters.add(filter);
        return this;
    }

    private TicketType[] validateAccessRight(RequestInfo requestInfo) throws UnauthorizedException {
        var allowed = new HashSet<TicketType>();
        if (requestInfo.userIsAuthorized(MANAGE_DOI)) {
            allowed.add(TicketType.DOI_REQUEST);
        }
        if (requestInfo.userIsAuthorized(AccessRight.SUPPORT)) {
            allowed.add(TicketType.GENERAL_SUPPORT_CASE);
        }
        if (requestInfo.userIsAuthorized(MANAGE_PUBLISHING_REQUESTS)) {
            allowed.add(TicketType.PUBLISHING_REQUEST);
        }
        if (allowed.isEmpty()) {
            allowed.add(TicketType.NONE);       // either set filter = none OR throw UnauthorizedException
            throw new UnauthorizedException();
        }
        return allowed.toArray(TicketType[]::new);
    }

    private boolean isUserOnly(TicketType... ticketTypes) {
        return Arrays.stream(ticketTypes).allMatch(pre -> pre.equals(TicketType.GENERAL_SUPPORT_CASE));
    }

    @Override
    protected Integer getFrom() {
        return parameters().get(FROM).as();
    }

    @Override
    protected Integer getSize() {
        return parameters().get(SIZE).as();
    }

    @Override
    protected TicketParameter getFieldsKey() {
        return FIELDS;
    }

    @Override
    protected TicketParameter getSortOrderKey() {
        return SORT_ORDER;
    }

    @Override
    protected TicketParameter getSearchAfterKey() {
        return SEARCH_AFTER;
    }

    @Override
    protected TicketParameter keyFromString(String keyName) {
        return TicketParameter.keyFromString(keyName);
    }

    @Override
    protected String getSortFieldName(Entry<String, SortOrder> entry) {
        return fromSortKey(entry.getKey()).jsonPath();
    }

    @Override
    public AsType<TicketParameter> getSort() {
        return parameters().get(SORT);
    }

    @Override
    public URI getOpenSearchUri() {
        return
            fromUri(openSearchUri)
                .addChild(TICKETS, SEARCH)
                .getUri();
    }

    @Override
    protected FilterAggregationBuilder getAggregationsWithFilter() {
        var aggrFilter = AggregationBuilders.filter(POST_FILTER, filters.get());
        getTicketsAggregations(currentUser)
            .stream().filter(this::isRequestedAggregation)
            .forEach(aggrFilter::subAggregation);
        return aggrFilter;
    }

    @Override
    protected Map<String, String> aggregationsDefinition() {
        return facetTicketsPaths;
    }

    @JacocoGenerated    // default value shouldn't happen, (developer have forgotten to handle a key)
    @Override
    protected Stream<Entry<TicketParameter, QueryBuilder>> customQueryBuilders(TicketParameter key) {
        return switch (key) {
            case ASSIGNEE -> byAssignee();
            default -> throw new IllegalArgumentException("unhandled key -> " + key.name());
        };
    }

    @Override
    protected boolean isDefined(String keyName) {
        return parameters().get(AGGREGATION)
            .asSplitStream(COMMA)
            .map(String::toLowerCase)
            .anyMatch(name -> name.equalsIgnoreCase(ALL) ||
                              name.equalsIgnoreCase(keyName) ||
                              isNotificationAggregation(keyName.toLowerCase(Locale.getDefault()))
            );
    }

    private boolean isNotificationAggregation(String name) {
        return name.contains("notification");
    }

    private Stream<Entry<TicketParameter, QueryBuilder>> byAssignee() {
        var searchByUserName = parameters().isPresent(BY_USER_PENDING) //override assignee if <user pending> is used
            ? currentUser
                : parameters().get(TicketParameter.ASSIGNEE).toString();

        return new OpensearchQueryText<TicketParameter>()
            .buildQuery(TicketParameter.ASSIGNEE, searchByUserName);
    }

    @SuppressWarnings("PMD.GodClass")
    public static class TicketParameterValidator extends ParameterValidator<TicketParameter, TicketQuery> {

        TicketParameterValidator() {
            super(new TicketQuery());
        }

        @Override
        protected void assignDefaultValues() {
            requiredMissing().forEach(key -> {
                switch (key) {
                    case FROM -> setValue(key.name(), DEFAULT_OFFSET);
                    case SIZE -> setValue(key.name(), DEFAULT_VALUE_PER_PAGE);
                    case SORT -> setValue(key.name(), DEFAULT_TICKET_SORT + COLON + DEFAULT_SORT_ORDER);
                    case AGGREGATION -> setValue(key.name(), ALL);
                    default -> { /* ignore and continue */ }
                }
            });
        }

        @JacocoGenerated
        @Override
        protected void applyRulesAfterValidation() {
            // convert page to offset if offset is not set
            if (query.parameters().isPresent(PAGE)) {
                if (query.parameters().isPresent(FROM)) {
                    var page = query.parameters().get(PAGE).<Number>as();
                    var perPage = query.parameters().get(SIZE).<Number>as();
                    query.parameters().set(FROM, String.valueOf(page.longValue() * perPage.longValue()));
                }
                query.parameters().remove(PAGE);
            }
            if (query.parameters().isPresent(BY_USER_PENDING)) {
                query.parameters().set(TicketParameter.TYPE, query.parameters().get(BY_USER_PENDING).as());
                query.parameters().set(STATUS, PENDING.toString());
            }
        }

        @Override
        protected Collection<String> validKeys() {
            return TICKET_PARAMETER_SET.stream()
                    .map(Enum::name)
                    .toList();
        }

        @Override
        protected void validateSortEntry(Entry<String, SortOrder> entry) {
            if (fromSortKey(entry.getKey()) == INVALID) {
                throw new IllegalArgumentException(
                    INVALID_VALUE_WITH_SORT.formatted(entry.getKey(), validSortKeys())
                );
            }
            attempt(entry::getValue)
                .orElseThrow(e -> new IllegalArgumentException(e.getException().getMessage()));
        }

        @Override
        protected void setValue(String key, String value) {
            var qpKey = TicketParameter.keyFromString(key);
            var decodedValue = qpKey.valueEncoding() != ValueEncoding.NONE
                    ? decodeUTF(value)
                    : value;
            switch (qpKey) {
                case INVALID -> invalidKeys.add(key);
                case SEARCH_AFTER, FROM, SIZE, PAGE -> query.parameters().set(qpKey, decodedValue);
                case FIELDS -> query.parameters().set(qpKey, ignoreInvalidFields(decodedValue));
                case AGGREGATION -> query.parameters().set(qpKey, ignoreInvalidAggregations(decodedValue));
                case SORT -> mergeToKey(SORT, trimSpace(decodedValue));
                case SORT_ORDER -> mergeToKey(SORT, decodedValue);
                case CREATED_DATE, MODIFIED_DATE, PUBLICATION_MODIFIED_DATE ->
                        query.parameters().set(qpKey, expandYearToDate(decodedValue));
                default -> mergeToKey(qpKey, decodedValue);
            }
        }

        @Override
        protected boolean isKeyValid(String keyName) {
            return TicketParameter.keyFromString(keyName) != TicketParameter.INVALID;
        }

        @Override
        protected boolean isAggregationValid(String aggregationName) {
            return
                    ALL.equalsIgnoreCase(aggregationName) ||
                            NONE.equalsIgnoreCase(aggregationName) ||
                            getTicketsAggregations("").stream()
                                    .anyMatch(builder -> builder.getName().equalsIgnoreCase(aggregationName));
        }
    }
}