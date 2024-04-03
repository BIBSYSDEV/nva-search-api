package no.unit.nva.search2.common;

import static com.google.common.net.MediaType.CSV_UTF_8;
import static com.google.common.net.MediaType.JSON_UTF_8;
import static java.util.Objects.nonNull;
import static no.unit.nva.search2.common.QueryTools.hasContent;
import static no.unit.nva.search2.common.constant.Defaults.DEFAULT_SORT_ORDER;
import static no.unit.nva.search2.common.constant.Functions.readSearchInfrastructureApiUri;
import static no.unit.nva.search2.common.constant.Patterns.COLON_OR_SPACE;
import static no.unit.nva.search2.common.constant.Patterns.PATTERN_IS_URL_PARAM_INDICATOR;
import static no.unit.nva.search2.common.constant.Words.ALL;
import static no.unit.nva.search2.common.constant.Words.ASTERISK;
import static no.unit.nva.search2.common.constant.Words.COMMA;
import static no.unit.nva.search2.common.constant.Words.KEYWORD_FALSE;
import static no.unit.nva.search2.common.enums.FieldOperator.NOT_ONE_ITEM;
import static no.unit.nva.search2.common.enums.FieldOperator.NO_ITEMS;
import static nva.commons.core.attempt.Try.attempt;
import static nva.commons.core.paths.UriWrapper.fromUri;
import com.google.common.net.MediaType;
import java.net.URI;
import java.util.Locale;
import java.util.Map.Entry;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import no.unit.nva.search.CsvTransformer;
import no.unit.nva.search2.common.builder.OpensearchQueryFuzzyKeyword;
import no.unit.nva.search2.common.builder.OpensearchQueryKeyword;
import no.unit.nva.search2.common.builder.OpensearchQueryRange;
import no.unit.nva.search2.common.builder.OpensearchQueryText;
import no.unit.nva.search2.common.constant.Words;
import no.unit.nva.search2.common.enums.ParameterKey;
import no.unit.nva.search2.common.enums.SortKey;
import no.unit.nva.search2.common.records.PagedSearch;
import no.unit.nva.search2.common.records.PagedSearchBuilder;
import no.unit.nva.search2.common.records.QueryContentWrapper;
import no.unit.nva.search2.common.records.SwsResponse;
import nva.commons.core.JacocoGenerated;
import org.opensearch.index.query.BoolQueryBuilder;
import org.opensearch.index.query.MultiMatchQueryBuilder.Type;
import org.opensearch.index.query.Operator;
import org.opensearch.index.query.QueryBuilder;
import org.opensearch.index.query.QueryBuilders;
import org.opensearch.search.aggregations.AggregationBuilder;
import org.opensearch.search.builder.SearchSourceBuilder;
import org.opensearch.search.sort.FieldSortBuilder;
import org.opensearch.search.sort.SortBuilders;
import org.opensearch.search.sort.SortOrder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@SuppressWarnings("PMD.GodClass")
public abstract class Query<K extends Enum<K> & ParameterKey> {

    protected static final Logger logger = LoggerFactory.getLogger(Query.class);
    public static final String LAST = "_last";
    protected transient URI openSearchUri = URI.create(readSearchInfrastructureApiUri());
    private transient MediaType mediaType;
    private transient URI gatewayUri = URI.create("https://unset/resource/search");

    protected final transient QueryTools<K> queryTools;
    private final transient QueryKeys<K> queryKeys;
    protected final transient QueryFilter filters;

    protected abstract Integer getFrom();

    protected abstract Integer getSize();

    protected abstract K getFieldsKey();

    protected abstract K getSortOrderKey();

    protected abstract K getSearchAfterKey();

    protected abstract K keyFromString(String keyName);

    protected abstract SortKey fromSortKey(String sortName);

    protected abstract AsType<K> getSort();

    /**
     * Builds URI to query SWS based on post body.
     *
     * @return an URI to Sws search without parameters.
     */
    protected abstract URI getOpenSearchUri();

    protected abstract AggregationBuilder getAggregationsWithFilter();

    protected abstract Map<String, String> aggregationsDefinition();

    @JacocoGenerated    // default value shouldn't happen, (developer have forgotten to handle a key)
    protected abstract Stream<Entry<K, QueryBuilder>> customQueryBuilders(K key);

    protected abstract boolean isDefined(String keyName);


    protected Query() {
        queryTools = new QueryTools<>();
        queryKeys = new QueryKeys<>(getFieldsKey(), getSortOrderKey());
        filters = new QueryFilter();
        setMediaType(MediaType.JSON_UTF_8.toString());
    }

    public <R, Q extends Query<K>> String doSearch(OpenSearchClient<R, Q> queryClient) {
        logSearchKeys();
        final var response = (SwsResponse) queryClient.doSearch((Q) this);
        return CSV_UTF_8.is(this.getMediaType())
            ? toCsvText(response)
            : toPagedResponse(response).toJsonString();
    }

    public Stream<QueryContentWrapper> createQueryBuilderStream() {
        var queryBuilder =
            parameters().getSearchKeys().findAny().isEmpty()
                ? QueryBuilders.matchAllQuery()
                : makeBoolQuery();

        var builder = defaultSearchSourceBuilder(queryBuilder);

        handleSearchAfter(builder);

        getSortStream().forEach(builder::sort);

        if (getMediaType().is(JSON_UTF_8)) {
            builder.aggregation(getAggregationsWithFilter());
        }
        logger.debug(builder.toString());
        return Stream.of(new QueryContentWrapper(builder, this.getOpenSearchUri()));
    }

    public QueryKeys<K> parameters() {
        return queryKeys;
    }

    public PagedSearch toPagedResponse(SwsResponse response) {
        final var requestParameter = parameters().asMap();
        final var source = URI.create(getNvaSearchApiUri().toString().split(PATTERN_IS_URL_PARAM_INDICATOR)[0]);
        final var aggregationFormatted = AggregationFormat.apply(response.aggregations(), aggregationsDefinition())
            .toString();

        return
            new PagedSearchBuilder()
                .withTotalHits(response.getTotalSize())
                .withHits(response.getSearchHits())
                .withIds(source, requestParameter, getFrom(), getSize())
                .withNextResultsBySortKey(nextResultsBySortKey(response, requestParameter, source))
                .withAggregations(aggregationFormatted)
                .build();
    }

    protected String toCsvText(SwsResponse response) {
        return CsvTransformer.transform(response.getSearchHits());
    }

    protected MediaType getMediaType() {
        return mediaType;
    }

    final void setMediaType(String mediaType) {
        if (nonNull(mediaType) && mediaType.contains(Words.TEXT_CSV)) {
            this.mediaType = CSV_UTF_8;
        } else {
            this.mediaType = JSON_UTF_8;
        }
    }

    public URI getNvaSearchApiUri() {
        return gatewayUri;
    }

    @JacocoGenerated
    public void setNvaSearchApiUri(URI gatewayUri) {
        this.gatewayUri = gatewayUri;
    }

    protected void setOpenSearchUri(URI openSearchUri) {
        this.openSearchUri = openSearchUri;
    }

    /**
     * Creates a multi match query, all words needs to be present, within a document.
     *
     * @return a MultiMatchQueryBuilder
     */
    protected Map<String, Float> fieldsToKeyNames(AsType<K> fieldValue) {
        return fieldValue.isEmpty() || fieldValue.asLowerCase().contains(ALL)
            ? Map.of(ASTERISK, 1F)       // NONE or ALL -> <'*',1.0>
            : fieldValue.asSplitStream(COMMA)
            .map(this::keyFromString)
            .flatMap(key -> key.searchFields(KEYWORD_FALSE)
                .map(jsonPath -> Map.entry(jsonPath, key.fieldBoost()))
            )
            .collect(Collectors.toMap(Entry::getKey, Entry::getValue));
    }

    protected Stream<FieldSortBuilder> getSortStream() {
        return getSort().asSplitStream(COMMA)
            .flatMap(item -> {
                final var parts = item.split(COLON_OR_SPACE);
                final var order = SortOrder.fromString(
                    attempt(() -> parts[1]).orElse((f) -> DEFAULT_SORT_ORDER));
                return fromSortKey(parts[0]).jsonPaths()
                    .map(path -> SortBuilders.fieldSort(path).order(order).missing(LAST));
            });
    }

    protected Stream<Entry<K, QueryBuilder>> getQueryBuilders(K key) {
        final var value = parameters().get(key).toString();
        return switch (key.fieldType()) {
            case DATE, NUMBER -> new OpensearchQueryRange<K>().buildQuery(key, value);
            case KEYWORD -> new OpensearchQueryKeyword<K>().buildQuery(key, value);
            case FUZZY_KEYWORD -> new OpensearchQueryFuzzyKeyword<K>().buildQuery(key, value);
            case TEXT, FUZZY_TEXT -> new OpensearchQueryText<K>().buildQuery(key, value);
            case FREE_TEXT -> queryTools.queryToEntry(key, multiMatchQuery(key, getFieldsKey()));
            case CUSTOM -> customQueryBuilders(key);
            case IGNORED -> Stream.empty();
            default -> {
                logger.info("default handling -> {}", key.name());
                yield new OpensearchQueryKeyword<K>().buildQuery(key, value);
            }
        };
    }

    protected BoolQueryBuilder makeBoolQuery() {
        return mainQuery();
    }

    /**
     * Creates a boolean query, with all the search parameters.
     *
     * @return a BoolQueryBuilder
     */
    protected BoolQueryBuilder mainQuery() {
        var boolQueryBuilder = QueryBuilders.boolQuery();
        parameters().getSearchKeys()
            .flatMap(this::getQueryBuilders)
            .forEach(entry -> {
                if (isMustNot(entry.getKey())) {
                    boolQueryBuilder.mustNot(entry.getValue());
                } else {
                    boolQueryBuilder.must(entry.getValue());
                }
            });
        return boolQueryBuilder;
    }

    protected SearchSourceBuilder defaultSearchSourceBuilder(QueryBuilder queryBuilder) {
        return new SearchSourceBuilder()
            .query(queryBuilder)
            .size(getSize())
            .from(getFrom())
            .postFilter(filters.get())
            .trackTotalHits(true);
    }

    protected QueryBuilder multiMatchQuery(K searchAllKey, K fieldsKey) {
        var fields = fieldsToKeyNames(parameters().get(fieldsKey));
        var value = parameters().get(searchAllKey).toString();
        return QueryBuilders
            .multiMatchQuery(value)
            .fields(fields)
            .type(Type.CROSS_FIELDS)
            .operator(Operator.AND);
    }

    protected boolean isRequestedAggregation(AggregationBuilder aggregationBuilder) {
        return Optional.ofNullable(aggregationBuilder)
            .map(AggregationBuilder::getName)
            .map(this::isDefined)
            .orElse(false);
    }

    private boolean isMustNot(K key) {
        return NO_ITEMS.equals(key.searchOperator())
            || NOT_ONE_ITEM.equals(key.searchOperator());
    }

    private URI nextResultsBySortKey(SwsResponse response, Map<String, String> requestParameter, URI gatewayUri) {
        requestParameter.remove(Words.FROM);
        var sortParameter =
            response.getSort().stream()
                .map(Object::toString)
                .collect(Collectors.joining(COMMA));
        if (!hasContent(sortParameter)) {
            return null;
        }
        var searchAfter = Words.SEARCH_AFTER.toLowerCase(Locale.getDefault());
        requestParameter.put(searchAfter, sortParameter);
        return fromUri(gatewayUri)
            .addQueryParameters(requestParameter)
            .getUri();
    }

    private void logSearchKeys() {
        logger.info(
            parameters().getSearchKeys()
                .map(ParameterKey::asCamelCase)
                .collect(Collectors.joining("\", \"", "{\"keys\":[\"", "\"]}"))
        );
    }

    private void handleSearchAfter(SearchSourceBuilder builder) {
        var sortKeys = parameters().remove(getSearchAfterKey()).split(COMMA);
        if (nonNull(sortKeys)) {
            builder.searchAfter(sortKeys);
        }
    }
}
