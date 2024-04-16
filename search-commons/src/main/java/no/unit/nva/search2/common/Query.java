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
import static no.unit.nva.search2.common.constant.Words.POST_FILTER;
import static no.unit.nva.search2.common.constant.Words.SORT_LAST;
import static no.unit.nva.search2.common.enums.FieldOperator.NOT_ONE_ITEM;
import static no.unit.nva.search2.common.enums.FieldOperator.NO_ITEMS;
import static nva.commons.core.attempt.Try.attempt;
import static nva.commons.core.paths.UriWrapper.fromUri;
import com.google.common.net.MediaType;
import java.net.URI;
import java.time.Instant;
import java.util.List;
import java.util.Locale;
import java.util.Map.Entry;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;
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
import org.opensearch.search.aggregations.AggregationBuilders;
import org.opensearch.search.builder.SearchSourceBuilder;
import org.opensearch.search.sort.FieldSortBuilder;
import org.opensearch.search.sort.SortBuilders;
import org.opensearch.search.sort.SortOrder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@SuppressWarnings("PMD.GodClass")
public abstract class Query<K extends Enum<K> & ParameterKey> {

    protected static final Logger logger = LoggerFactory.getLogger(Query.class);
    protected transient URI openSearchUri = URI.create(readSearchInfrastructureApiUri());
    private transient MediaType mediaType;
    private transient URI gatewayUri = URI.create("https://unset/resource/search");

    protected final transient QueryFilter filters;
    protected final transient QueryTools<K> queryTools;
    private final transient QueryKeys<K> queryKeys;
    private final transient Instant startTime;


    protected abstract AsType<K> getFrom();

    protected abstract AsType<K> getSize();

    protected abstract AsType<K> getSort();

    protected abstract K keyAggregation();

    protected abstract K keyFields();

    protected abstract K keySortOrder();

    protected abstract K keySearchAfter();

    protected abstract K toKey(String keyName);

    protected abstract SortKey toSortKey(String sortName);


    /**
     * Builds URI to query SWS based on post body.
     *
     * @return an URI to Sws search without parameters.
     */
    protected abstract URI getOpenSearchUri();

    protected abstract String toCsvText(SwsResponse response);
    protected abstract void setFetchSource(SearchSourceBuilder builder);


    /**
     * Path to each and every facet defined in  builderAggregations().
     *
     * @return MapOf(Name, Path)
     */
    protected abstract Map<String, String> facetPaths();

    protected abstract List<AggregationBuilder> builderAggregations();

    protected abstract Stream<Entry<K, QueryBuilder>> builderStreamCustomQuery(K key);

    protected Query() {
        startTime = Instant.now();
        queryTools = new QueryTools<>();
        queryKeys = new QueryKeys<>(keyFields(), keySortOrder());
        filters = new QueryFilter();
        setMediaType(JSON_UTF_8.toString());
    }

    public <R, Q extends Query<K>> String doSearch(OpenSearchClient<R, Q> queryClient) {
        logSearchKeys();
        final var response = (SwsResponse) queryClient.doSearch((Q) this);
        return CSV_UTF_8.is(this.getMediaType())
            ? toCsvText(response)
            : toPagedResponse(response).toJsonString();
    }

    public <R, Q extends Query<K>> String doExport(OpenSearchClient<R, Q> queryClient) {
        logSearchKeys();
        final var response = (SwsResponse) queryClient.doSearch((Q) this);
        return toCsvText(response);
    }

    public Instant getStartTime() {
        return startTime;
    }

    public PagedSearch toPagedResponse(SwsResponse response) {
        final var requestParameter = parameters().asMap();
        final var source = URI.create(getNvaSearchApiUri().toString().split(PATTERN_IS_URL_PARAM_INDICATOR)[0]);
        final var aggregationFormatted = AggregationFormat.apply(response.aggregations(), facetPaths())
            .toString();

        return
            new PagedSearchBuilder()
                .withTotalHits(response.getTotalSize())
                .withHits(response.getSearchHits())
                .withIds(source, requestParameter, getFrom().as(), getSize().as())
                .withNextResultsBySortKey(nextResultsBySortKey(response, requestParameter, source))
                .withAggregations(aggregationFormatted)
                .build();
    }


    public QueryKeys<K> parameters() {
        return queryKeys;
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
            .map(this::toKey)
            .flatMap(key -> key.searchFields(KEYWORD_FALSE)
                .map(jsonPath -> Map.entry(jsonPath, key.fieldBoost()))
            )
            .collect(Collectors.toMap(Entry::getKey, Entry::getValue));
    }

    @JacocoGenerated        // default can only be tested if we add a new fieldtype not in use....
    protected Stream<Entry<K, QueryBuilder>> builderStreamDefaultQuery(K key) {
        final var value = parameters().get(key).toString();
        return switch (key.fieldType()) {
            case DATE, NUMBER -> new OpensearchQueryRange<K>().buildQuery(key, value);
            case KEYWORD -> new OpensearchQueryKeyword<K>().buildQuery(key, value);
            case FUZZY_KEYWORD -> new OpensearchQueryFuzzyKeyword<K>().buildQuery(key, value);
            case TEXT, FUZZY_TEXT -> new OpensearchQueryText<K>().buildQuery(key, value);
            case FREE_TEXT -> queryTools.queryToEntry(key, builderSearchAllQuery(key));
            case CUSTOM -> builderStreamCustomQuery(key);
            case IGNORED -> Stream.empty();
            default -> throw new RuntimeException("handler NOT defined -> " + key.name());
        };
    }

    public Stream<QueryContentWrapper> assemble() {
        var queryBuilder =
            parameters().getSearchKeys().findAny().isEmpty()
                ? QueryBuilders.matchAllQuery()
                : builderMainQuery();

        var builder = builderDefaultSearchSource(queryBuilder);

        handleSearchAfter(builder);

        builderStreamFieldSort().forEach(builder::sort);

        if (includeAggregation()) {
            builder.aggregation(builderAggregationsWithFilter());
        }
        logger.debug(builder.toString());
        return Stream.of(new QueryContentWrapper(builder, this.getOpenSearchUri()));
    }

    /**
     * Creates a boolean query, with all the search parameters.
     *
     * @return a BoolQueryBuilder
     */
    protected BoolQueryBuilder builderMainQuery() {
        var boolQueryBuilder = QueryBuilders.boolQuery();
        parameters().getSearchKeys()
            .flatMap(this::builderStreamDefaultQuery)
            .forEach(entry -> {
                if (isMustNot(entry.getKey())) {
                    boolQueryBuilder.mustNot(entry.getValue());
                } else {
                    boolQueryBuilder.must(entry.getValue());
                }
            });
        return boolQueryBuilder;
    }

    protected Stream<FieldSortBuilder> builderStreamFieldSort() {
        return getSort().asSplitStream(COMMA)
            .flatMap(item -> {
                final var parts = item.split(COLON_OR_SPACE);
                final var order = SortOrder.fromString(
                    attempt(() -> parts[1]).orElse((f) -> DEFAULT_SORT_ORDER));
                return toSortKey(parts[0]).jsonPaths()
                    .map(path -> SortBuilders.fieldSort(path).order(order).missing(SORT_LAST));
            });
    }

    protected AggregationBuilder builderAggregationsWithFilter() {
        var aggrFilter = AggregationBuilders.filter(POST_FILTER, filters.get());
        builderAggregations().forEach(aggrFilter::subAggregation);
        return aggrFilter;
    }

    protected SearchSourceBuilder builderDefaultSearchSource(QueryBuilder queryBuilder) {
        return new SearchSourceBuilder()
            .query(queryBuilder)
            .size(getSize().as())
            .from(getFrom().as())
            .postFilter(filters.get())
            .trackTotalHits(true);
    }

    protected QueryBuilder builderSearchAllQuery(K searchAllKey) {
        var fields = fieldsToKeyNames(parameters().get(keyFields()));
        var value = parameters().get(searchAllKey).toString();
        return QueryBuilders
            .multiMatchQuery(value)
            .fields(fields)
            .type(Type.CROSS_FIELDS)
            .operator(Operator.AND);
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

    private boolean includeAggregation() {
        return getMediaType().is(JSON_UTF_8) && ALL.equalsIgnoreCase(parameters().get(keyAggregation()).as());
    }

    private boolean isMustNot(K key) {
        return NO_ITEMS.equals(key.searchOperator())
            || NOT_ONE_ITEM.equals(key.searchOperator());
    }

    private void logSearchKeys() {
        logger.info(
            parameters().getSearchKeys()
                .map(ParameterKey::asCamelCase)
                .collect(Collectors.joining("\", \"", "{\"keys\":[\"", "\"]}"))
        );
    }

    private void handleSearchAfter(SearchSourceBuilder builder) {
        var sortKeys = parameters().remove(keySearchAfter()).split(COMMA);
        if (nonNull(sortKeys)) {
            builder.searchAfter(sortKeys);
        }
        setFetchSource(builder);
    }
}
