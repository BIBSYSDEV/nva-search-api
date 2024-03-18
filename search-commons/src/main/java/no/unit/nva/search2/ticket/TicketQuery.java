package no.unit.nva.search2.ticket;

import static java.util.Objects.nonNull;
import static no.unit.nva.search2.common.QueryTools.decodeUTF;
import static no.unit.nva.search2.common.constant.Defaults.DEFAULT_OFFSET;
import static no.unit.nva.search2.common.constant.Defaults.DEFAULT_SORT_ORDER;
import static no.unit.nva.search2.common.constant.Defaults.DEFAULT_VALUE_PER_PAGE;
import static no.unit.nva.search2.common.constant.ErrorMessages.INVALID_VALUE_WITH_SORT;
import static no.unit.nva.search2.common.constant.Words.ALL;
import static no.unit.nva.search2.common.constant.Words.COLON;
import static no.unit.nva.search2.common.constant.Words.COMMA;
import static no.unit.nva.search2.common.constant.Words.ID;
import static no.unit.nva.search2.common.constant.Words.POST_FILTER;
import static no.unit.nva.search2.common.constant.Words.SEARCH;
import static no.unit.nva.search2.common.constant.Words.TICKETS;
import static no.unit.nva.search2.common.constant.Words.TYPE;
import static no.unit.nva.search2.common.enums.TicketStatus.PENDING;
import static no.unit.nva.search2.ticket.Constants.DEFAULT_TICKET_SORT;
import static no.unit.nva.search2.ticket.Constants.ORGANIZATION;
import static no.unit.nva.search2.ticket.Constants.ORGANIZATION_ID_KEYWORD;
import static no.unit.nva.search2.ticket.Constants.TYPE_KEYWORD;
import static no.unit.nva.search2.ticket.Constants.facetTicketsPaths;
import static no.unit.nva.search2.ticket.Constants.getTicketsAggregations;
import static no.unit.nva.search2.ticket.TicketParameter.AGGREGATION;
import static no.unit.nva.search2.ticket.TicketParameter.BY_USER_PENDING;
import static no.unit.nva.search2.ticket.TicketParameter.FIELDS;
import static no.unit.nva.search2.ticket.TicketParameter.FROM;
import static no.unit.nva.search2.ticket.TicketParameter.PAGE;
import static no.unit.nva.search2.ticket.TicketParameter.SEARCH_AFTER;
import static no.unit.nva.search2.ticket.TicketParameter.SIZE;
import static no.unit.nva.search2.ticket.TicketParameter.SORT;
import static no.unit.nva.search2.ticket.TicketParameter.SORT_ORDER;
import static no.unit.nva.search2.ticket.TicketParameter.STATUS;
import static no.unit.nva.search2.ticket.TicketSort.INVALID;
import static no.unit.nva.search2.ticket.TicketSort.fromSortKey;
import static no.unit.nva.search2.ticket.TicketSort.validSortKeys;
import static nva.commons.core.attempt.Try.attempt;
import static nva.commons.core.paths.UriWrapper.fromUri;
import java.net.URI;
import java.util.Arrays;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Stream;
import no.unit.nva.search2.common.AsType;
import no.unit.nva.search2.common.ParameterValidator;
import no.unit.nva.search2.common.Query;
import no.unit.nva.search2.common.builder.OpensearchQueryText;
import no.unit.nva.search2.common.enums.ValueEncoding;
import no.unit.nva.search2.common.records.QueryContentWrapper;
import nva.commons.core.JacocoGenerated;
import org.opensearch.index.query.QueryBuilder;
import org.opensearch.index.query.QueryBuilders;
import org.opensearch.index.query.TermQueryBuilder;
import org.opensearch.index.query.TermsQueryBuilder;
import org.opensearch.search.aggregations.AggregationBuilder;
import org.opensearch.search.aggregations.AggregationBuilders;
import org.opensearch.search.aggregations.bucket.filter.FilterAggregationBuilder;
import org.opensearch.search.builder.SearchSourceBuilder;
import org.opensearch.search.sort.SortOrder;

public final class TicketQuery extends Query<TicketParameter> {

    private String username;
    private TicketQuery() {
        super();
        assignImpossibleWhiteListFilters();
    }

    @JacocoGenerated    // default value shouldn't happen, (developer have forgotten to handle a key)
    @Override
    protected Stream<Entry<TicketParameter, QueryBuilder>> customQueryBuilders(TicketParameter key) {
        return switch (key) {
            case ASSIGNEE -> byUserPending();
            default -> throw new IllegalArgumentException("unhandled key -> " + key.name());
        };
    }

    @Override
    protected TicketParameter keyFromString(String keyName) {
        return TicketParameter.keyFromString(keyName);
    }

    private Stream<Entry<TicketParameter, QueryBuilder>> byUserPending() {
        var searchByUserName = isPresent(BY_USER_PENDING)
            ? username
            : getValue(TicketParameter.ASSIGNEE).toString();

        return new OpensearchQueryText<TicketParameter>()
            .buildQuery(TicketParameter.ASSIGNEE, searchByUserName);
    }

    public static TicketParameterValidator builder() {
        return new TicketParameterValidator();
    }

    @Override
    protected Integer getFrom() {
        return getValue(FROM).as();
    }

    @Override
    protected Integer getSize() {
        return getValue(SIZE).as();
    }

    @Override
    protected TicketParameter getFieldsKey() {
        return FIELDS;
    }


    @Override
    public AsType<TicketParameter> getSort() {
        return getValue(SORT);
    }

    @Override
    public URI getOpenSearchUri() {
        return
            fromUri(openSearchUri)
                .addChild(TICKETS, SEARCH)
                .getUri();
    }

    @Override
    protected boolean isPagingValue(TicketParameter key) {
        return key.ordinal() >= FIELDS.ordinal() && key.ordinal() <= SORT_ORDER.ordinal();
    }

    @Override
    protected Map<String, String> aggregationsDefinition() {
        return facetTicketsPaths;
    }

    public TicketQuery withUser(String username) {
        this.username = username;
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
    public TicketQuery withRequiredTicketType(TicketType... ticketTypes) {
        var ticketStringTypes = Arrays.stream(ticketTypes).map(Object::toString).toList();
        final var filter = new TermsQueryBuilder(TYPE_KEYWORD, ticketStringTypes)
            .queryName(TICKETS + TYPE);
        this.addFilter(filter);
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
    public TicketQuery withRequiredOrganization(URI organization) {
        final var filter = new TermQueryBuilder(ORGANIZATION_ID_KEYWORD, organization.toString())
            .queryName(ORGANIZATION + ID);
        this.addFilter(filter);
        return this;
    }

    public Stream<QueryContentWrapper> createQueryBuilderStream() {
        var queryBuilder =
            this.hasNoSearchValue()
                ? QueryBuilders.matchAllQuery()
                : mainQuery();

        var builder = defaultSearchSourceBuilder(queryBuilder);

        handleSearchAfter(builder);

        getSortStream()
            .forEach(entry -> builder.sort(getSortFieldName(entry), entry.getValue()));

        builder.aggregation(getAggregationsWithFilter());

        logger.info(builder.toString());

        return Stream.of(new QueryContentWrapper(builder, this.getOpenSearchUri()));
    }

    private FilterAggregationBuilder getAggregationsWithFilter() {
        var aggrFilter = AggregationBuilders.filter(POST_FILTER, getFilters());
        getTicketsAggregations(username)
            .stream().filter(this::isRequestedAggregation)
            .forEach(aggrFilter::subAggregation);
        return aggrFilter;
    }

    private boolean isRequestedAggregation(AggregationBuilder aggregationBuilder) {
        return Optional.ofNullable(aggregationBuilder)
            .map(AggregationBuilder::getName)
            .map(this::isDefined)
            .orElse(false);
    }

    private boolean isDefined(String keyName) {
        return getValue(AGGREGATION)
            .asSplitStream(COMMA)
            .map(String::toLowerCase)
            .anyMatch(name -> name.equalsIgnoreCase(ALL) ||
                              name.equalsIgnoreCase(keyName) ||
                              isNotificationAggregation(keyName.toLowerCase(Locale.getDefault()))
            );
    }

    private static boolean isNotificationAggregation(String name) {
        return name.contains("notification");
    }


    private String getSortFieldName(Entry<String, SortOrder> entry) {
        return fromSortKey(entry.getKey()).getFieldName();
    }

    private void handleSearchAfter(SearchSourceBuilder builder) {
        var sortKeys = removeKey(SEARCH_AFTER).split(COMMA);
        if (nonNull(sortKeys)) {
            builder.searchAfter(sortKeys);
        }
    }

    /**
     * Add a (default) filter to the query that will never match any document.
     *
     * <p>This whitelist the ResourceQuery from any forgetful developer (me)</p>
     * <p>i.e.In order to return any results, withRequiredStatus must be set </p>
     * <p>See  for the correct way to filter by status</p>
     */
    private void assignImpossibleWhiteListFilters() {
        var filterType =
            new TermsQueryBuilder(TYPE_KEYWORD, TicketType.NONE).queryName(TYPE + POST_FILTER);
        final var filterId =
            new TermQueryBuilder(ORGANIZATION_ID_KEYWORD, UUID.randomUUID()).queryName(ORGANIZATION + ID);
        setFilters(filterType, filterId);
    }

    @SuppressWarnings("PMD.GodClass")
    public static class TicketParameterValidator extends ParameterValidator<TicketParameter, TicketQuery> {

        TicketParameterValidator() {
            super(new TicketQuery());
        }

        @Override
        protected boolean isKeyValid(String keyName) {
            return TicketParameter.keyFromString(keyName) != TicketParameter.INVALID;
        }

        @Override
        protected boolean isAggregationValid(String aggregationName) {
            return getTicketsAggregations("").stream()
                .anyMatch(builder -> builder.getName().equalsIgnoreCase(aggregationName));
        }

        @Override
        protected void assignDefaultValues() {
            requiredMissing().forEach(key -> {
                switch (key) {
                    case FROM -> setValue(key.fieldName(), DEFAULT_OFFSET);
                    case SIZE -> setValue(key.fieldName(), DEFAULT_VALUE_PER_PAGE);
                    case SORT -> setValue(key.fieldName(), DEFAULT_TICKET_SORT + COLON + DEFAULT_SORT_ORDER);
                    case AGGREGATION -> setValue(key.fieldName(), ALL);
                    default -> { /* ignore and continue */ }
                }
            });
        }

        @Override
        protected void setValue(String key, String value) {
            var qpKey = TicketParameter.keyFromString(key);
            var decodedValue = qpKey.valueEncoding() != ValueEncoding.NONE
                ? decodeUTF(value)
                : value;
            switch (qpKey) {
                case INVALID -> invalidKeys.add(key);
                case SEARCH_AFTER, FROM, SIZE, PAGE -> query.setKeyValue(qpKey, decodedValue);
                case FIELDS -> query.setKeyValue(qpKey, ignoreInvalidFields(decodedValue));
                case AGGREGATION -> query.setKeyValue(qpKey, ignoreInvalidAggregations(decodedValue));
                case SORT -> mergeToKey(SORT, trimSpace(decodedValue));
                case SORT_ORDER -> mergeToKey(SORT, decodedValue);
                case CREATED_DATE, MODIFIED_DATE, PUBLICATION_MODIFIED_DATE ->
                    query.setKeyValue(qpKey, expandYearToDate(decodedValue));
                default -> mergeToKey(qpKey, decodedValue);
            }
        }

        @JacocoGenerated
        @Override
        protected void applyRulesAfterValidation() {
            // convert page to offset if offset is not set
            if (query.isPresent(PAGE)) {
                if (query.isPresent(FROM)) {
                    var page = query.getValue(PAGE).<Number>as();
                    var perPage = query.getValue(SIZE).<Number>as();
                    query.setKeyValue(FROM, String.valueOf(page.longValue() * perPage.longValue()));
                }
                query.removeKey(PAGE);
            }
            if (query.isPresent(BY_USER_PENDING)) {
                query.setKeyValue(TicketParameter.TYPE, query.getValue(BY_USER_PENDING).as());
                query.setKeyValue(STATUS, PENDING.toString());
            }
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
    }
}