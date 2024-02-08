package no.unit.nva.search2.ticket;

import java.net.URI;
import java.util.Arrays;
import java.util.Collection;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Stream;
import no.unit.nva.search2.common.Query;
import no.unit.nva.search2.common.QueryBuilder;
import no.unit.nva.search2.common.QueryContentWrapper;
import no.unit.nva.search2.constant.Words;
import no.unit.nva.search2.enums.ParameterKey;
import no.unit.nva.search2.enums.ParameterKey.ValueEncoding;
import no.unit.nva.search2.enums.PublicationStatus;
import nva.commons.core.JacocoGenerated;
import org.opensearch.index.query.QueryBuilders;
import org.opensearch.index.query.TermsQueryBuilder;
import org.opensearch.search.aggregations.AggregationBuilder;
import org.opensearch.search.aggregations.AggregationBuilders;
import org.opensearch.search.aggregations.bucket.filter.FilterAggregationBuilder;
import org.opensearch.search.builder.SearchSourceBuilder;
import org.opensearch.search.sort.SortOrder;

import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;
import static no.unit.nva.search2.common.QueryTools.decodeUTF;
import static no.unit.nva.search2.constant.Defaults.DEFAULT_OFFSET;
import static no.unit.nva.search2.constant.Defaults.DEFAULT_SORT_ORDER;
import static no.unit.nva.search2.constant.Defaults.DEFAULT_VALUE_PER_PAGE;
import static no.unit.nva.search2.constant.ErrorMessages.INVALID_VALUE_WITH_SORT;
import static no.unit.nva.search2.constant.Resource.DEFAULT_RESOURCE_SORT;
import static no.unit.nva.search2.constant.Resource.PUBLICATION_STATUS;
import static no.unit.nva.search2.constant.Resource.RESOURCES_AGGREGATIONS;
import static no.unit.nva.search2.constant.Words.ALL;
import static no.unit.nva.search2.constant.Words.ASTERISK;
import static no.unit.nva.search2.constant.Words.COLON;
import static no.unit.nva.search2.constant.Words.COMMA;
import static no.unit.nva.search2.constant.Words.DOT;
import static no.unit.nva.search2.constant.Words.FILTER;
import static no.unit.nva.search2.constant.Words.KEYWORD;
import static no.unit.nva.search2.constant.Words.STATUS;
import static no.unit.nva.search2.ticket.TicketParameter.AGGREGATION;
import static no.unit.nva.search2.ticket.TicketParameter.FIELDS;
import static no.unit.nva.search2.ticket.TicketParameter.FROM;
import static no.unit.nva.search2.ticket.TicketParameter.PAGE;
import static no.unit.nva.search2.ticket.TicketParameter.SEARCH_AFTER;
import static no.unit.nva.search2.ticket.TicketParameter.SIZE;
import static no.unit.nva.search2.ticket.TicketParameter.SORT;
import static no.unit.nva.search2.ticket.TicketParameter.SORT_ORDER;
import static no.unit.nva.search2.ticket.TicketParameter.keyFromString;
import static no.unit.nva.search2.ticket.TicketSort.INVALID;
import static no.unit.nva.search2.ticket.TicketSort.fromSortKey;
import static no.unit.nva.search2.ticket.TicketSort.validSortKeys;
import static nva.commons.core.StringUtils.EMPTY_STRING;
import static nva.commons.core.attempt.Try.attempt;
import static nva.commons.core.paths.UriWrapper.fromUri;

public final class TicketQuery extends Query<TicketParameter> {

    public static final float PI = 3.14F;        // π
    public static final float PHI = 1.618F;      // Golden Ratio (Φ) -> used in the future for boosting.

    private TicketQuery() {
        super();
        assignStatusImpossibleWhiteList();
    }

    public static TicketQueryBuilder builder() {
        return new TicketQueryBuilder();
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
    protected String[] fieldsToKeyNames(String field) {
        return ALL.equals(field) || isNull(field)
            ? ASTERISK.split(COMMA)     // NONE or ALL -> ['*']
            : Arrays.stream(field.split(COMMA))
                .map(TicketParameter::keyFromString)
                .map(ParameterKey::searchFields)
                .flatMap(Collection::stream)
                .map(fieldPath -> fieldPath.replace(DOT + KEYWORD, EMPTY_STRING))
                .toArray(String[]::new);
    }

    @Override
    public AsType getSort() {
        return getValue(SORT);
    }

    @Override
    public URI getOpenSearchUri() {
        return
            fromUri(openSearchUri)
                .addChild(Words.RESOURCES, Words.SEARCH)
                .getUri();
    }

    @Override
    protected boolean isPagingValue(TicketParameter key) {
        return key.ordinal() >= FIELDS.ordinal() && key.ordinal() <= SORT_ORDER.ordinal();
    }

    /**
     * Filter on Required Status.
     *
     * <p>Only STATUES specified here will be available for the Query.</p>
     * <p>This is to avoid the Query to return documents that are not available for the user.</p>
     * <p>See {@link PublicationStatus} for available values.</p>
     *
     * @param publicationStatus the required statues
     * @return ResourceQuery (builder pattern)
     */
    public TicketQuery withUserAccessFilter(PublicationStatus... publicationStatus) {
        final var values = Arrays.stream(publicationStatus)
            .map(PublicationStatus::toString)
            .toArray(String[]::new);
        final var filter = new TermsQueryBuilder(PUBLICATION_STATUS, values)
            .queryName(STATUS);
        this.addFilter(filter);
        return this;
    }

    public Stream<QueryContentWrapper> createQueryBuilderStream() {
        var queryBuilder =
            this.hasNoSearchValue()
                ? QueryBuilders.matchAllQuery()
                : boolQuery();

        var builder = getSourceBuilder(queryBuilder);

        handleSearchAfter(builder);

        getSortStream()
            .forEach(entry -> builder.sort(getSortFieldName(entry), entry.getValue()));

        builder.aggregation(getAggregationsWithFilter());

        logger.debug(builder.toString());

        return Stream.of(new QueryContentWrapper(builder, this.getOpenSearchUri()));
    }

    private FilterAggregationBuilder getAggregationsWithFilter() {
        var aggrFilter = AggregationBuilders.filter(FILTER, getFilters());
        RESOURCES_AGGREGATIONS
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

    private boolean isDefined(String key) {
        return getValue(AGGREGATION).optionalStream()
            .flatMap(item -> Arrays.stream(item.split(COMMA)).sequential())
            .anyMatch(name -> name.equals(ALL) || name.equals(key));
    }

    private String getSortFieldName(Entry<String, SortOrder> entry) {
        return fromSortKey(entry.getKey()).getFieldName();
    }

    private void handleSearchAfter(SearchSourceBuilder builder) {
        var searchAfter = removeKey(SEARCH_AFTER);
        if (nonNull(searchAfter)) {
            var sortKeys = searchAfter.split(COMMA);
            builder.searchAfter(sortKeys);
        }
    }

    /**
     * Add a (default) filter to the query that will never match any document.
     *
     * <p>This whitelist the ResourceQuery from any forgetful developer (me)</p>
     * <p>i.e.In order to return any results, withRequiredStatus must be set </p>
     * <p>See {@link #withUserAccessFilter(PublicationStatus...)} for the correct way to filter by status</p>
     */
    private void assignStatusImpossibleWhiteList() {
        setFilters(new TermsQueryBuilder(PUBLICATION_STATUS, UUID.randomUUID().toString()).queryName(STATUS));
    }

    @SuppressWarnings("PMD.GodClass")
    public static class TicketQueryBuilder extends QueryBuilder<TicketParameter, TicketQuery> {

        TicketQueryBuilder() {
            super(new TicketQuery());
        }

        @Override
        protected boolean isKeyValid(String keyName) {
            return keyFromString(keyName) != TicketParameter.INVALID;
        }

        @Override
        protected void assignDefaultValues() {
            requiredMissing().forEach(key -> {
                switch (key) {
                    case FROM -> setValue(key.fieldName(), DEFAULT_OFFSET);
                    case SIZE -> setValue(key.fieldName(), DEFAULT_VALUE_PER_PAGE);
                    case SORT -> setValue(key.fieldName(), DEFAULT_RESOURCE_SORT + COLON + DEFAULT_SORT_ORDER);
                    default -> { /* ignore and continue */ }
                }
            });
        }

        @Override
        protected void setValue(String key, String value) {
            var qpKey = keyFromString(key);
            var decodedValue = qpKey.valueEncoding() != ValueEncoding.NONE
                ? decodeUTF(value)
                : value;
            switch (qpKey) {
                case INVALID -> invalidKeys.add(key);
                case SEARCH_AFTER, FROM, SIZE, PAGE -> query.setKeyValue(qpKey, decodedValue);
                case FIELDS -> query.setKeyValue(qpKey, ignoreInvalidFields(decodedValue));
                case SORT -> mergeToKey(SORT, trimSpace(decodedValue));
                case SORT_ORDER -> mergeToKey(SORT, decodedValue);
                //                case PUBLISHED_SINCE -> query.setKeyValue(qpKey, expandYearToDate(decodedValue));
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