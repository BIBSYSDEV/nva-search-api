package no.unit.nva.search2.common;

import static com.google.common.net.MediaType.CSV_UTF_8;
import static com.google.common.net.MediaType.JSON_UTF_8;
import static java.util.Objects.nonNull;
import static no.unit.nva.search2.common.constant.Defaults.DEFAULT_SORT_ORDER;
import static no.unit.nva.search2.common.constant.Patterns.COLON_OR_SPACE;
import static no.unit.nva.search2.common.constant.Patterns.PATTERN_IS_URL_PARAM_INDICATOR;
import static no.unit.nva.search2.common.constant.Words.ALL;
import static no.unit.nva.search2.common.constant.Words.ASTERISK;
import static no.unit.nva.search2.common.constant.Words.COMMA;
import static no.unit.nva.search2.common.constant.Words.KEYWORD_FALSE;
import static no.unit.nva.search2.common.constant.Words.POST_FILTER;
import static no.unit.nva.search2.common.constant.Words.RELEVANCE_KEY_NAME;
import static no.unit.nva.search2.common.constant.Words.SORT_LAST;
import static no.unit.nva.search2.common.enums.FieldOperator.NOT_ONE_ITEM;
import static no.unit.nva.search2.common.enums.FieldOperator.NO_ITEMS;
import static nva.commons.core.attempt.Try.attempt;
import com.google.common.net.MediaType;
import java.net.URI;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import no.unit.nva.search2.common.builder.OpensearchQueryAcrossFields;
import no.unit.nva.search2.common.builder.OpensearchQueryFuzzyKeyword;
import no.unit.nva.search2.common.builder.OpensearchQueryKeyword;
import no.unit.nva.search2.common.builder.OpensearchQueryRange;
import no.unit.nva.search2.common.builder.OpensearchQueryText;
import no.unit.nva.search2.common.records.ResponseFormatter;
import no.unit.nva.search2.common.constant.Words;
import no.unit.nva.search2.common.enums.ParameterKey;
import no.unit.nva.search2.common.enums.SortKey;
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
import org.opensearch.search.sort.SortBuilder;
import org.opensearch.search.sort.SortBuilders;
import org.opensearch.search.sort.SortOrder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@SuppressWarnings("PMD.GodClass")
public abstract class SearchQuery<K extends Enum<K> & ParameterKey> extends Query<K> {

    protected static final Logger logger = LoggerFactory.getLogger(SearchQuery.class);
    private transient MediaType mediaType;
    private transient URI gatewayUri = URI.create("https://unset/resource/search");

    public final transient QueryFilter filters;
    protected final transient QueryTools<K> queryTools;


    protected abstract AsType<K> getFrom();

    protected abstract AsType<K> getSize();

    protected abstract AsType<K> getSort();

    protected abstract String getExclude();

    protected abstract String getInclude();

    protected abstract K keyAggregation();

    protected abstract K keyFields();

    protected abstract K keySortOrder();

    protected abstract K keySearchAfter();

    protected abstract K toKey(String keyName);

    protected abstract SortKey toSortKey(String sortName);

    /**
     * Path to each and every facet defined in  builderAggregations().
     *
     * @return MapOf(Name, Path)
     */
    protected abstract Map<String, String> facetPaths();

    protected abstract List<AggregationBuilder> builderAggregations();

    protected abstract Stream<Entry<K, QueryBuilder>> builderStreamCustomQuery(K key);

    protected SearchQuery() {
        super();
        queryTools = new QueryTools<>();
        filters = new QueryFilter();
        queryKeys = new QueryKeys<>(keyFields(), keySortOrder());
        setMediaType(JSON_UTF_8.toString());
    }

    @Override
    public <R, Q extends Query<K>> ResponseFormatter doSearch(OpenSearchClient<R, Q> queryClient) {
        final var requestParameter = parameters().asMap();
        final var source = URI.create(getNvaSearchApiUri().toString().split(PATTERN_IS_URL_PARAM_INDICATOR)[0]);
        return new ResponseFormatter(
            (SwsResponse) queryClient.doSearch((Q) this),
            getMediaType(),
            source,
            getFrom().as(),
            getSize().as(),
            facetPaths(),
            requestParameter);
    }

    public MediaType getMediaType() {
        return mediaType;
    }

    final void setMediaType(String mediaType) {
        if (nonNull(mediaType) && mediaType.contains(Words.TEXT_CSV)) {
            this.mediaType = CSV_UTF_8;
        } else {
            this.mediaType = JSON_UTF_8;
        }
    }

    /**
     * Builds URI to query SWS based on post body.
     *
     * @return an URI to Sws search without parameters.
     */
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
            case TEXT -> new OpensearchQueryText<K>().buildQuery(key, value);
            case FREE_TEXT -> queryTools.queryToEntry(key, builderSearchAllQuery(key));
            case ACROSS_FIELDS -> new OpensearchQueryAcrossFields<K>().buildQuery(key, value);
            case CUSTOM -> builderStreamCustomQuery(key);
            case IGNORED -> Stream.empty();
            default -> throw new RuntimeException("handler NOT defined -> " + key.name());
        };
    }

    @Override
    public Stream<QueryContentWrapper> assemble() {
        var queryBuilder =
            parameters().getSearchKeys().findAny().isEmpty()
                ? QueryBuilders.matchAllQuery()
                : builderMainQuery();

        var builder = builderDefaultSearchSource(queryBuilder);

        if (fetchSource()) {
            builder.fetchSource(getInclude(), getExclude());
        } else {
            builder.fetchSource(true);
        }

        handleSearchAfter(builder);

        builderStreamFieldSort().forEach(builder::sort);

        if (includeAggregation()) {
            builder.aggregation(builderAggregationsWithFilter());
        }

        return Stream.of(new QueryContentWrapper(builder.toString(), this.getOpenSearchUri()));
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

    protected Stream<SortBuilder<?>> builderStreamFieldSort() {
        return getSort().asSplitStream(COMMA)
            .flatMap(item -> {
                final var parts = item.split(COLON_OR_SPACE);
                final var order = SortOrder.fromString(
                    attempt(() -> parts[1]).orElse((f) -> DEFAULT_SORT_ORDER));
                final var sortKey = toSortKey(parts[0]);

                return RELEVANCE_KEY_NAME.equalsIgnoreCase(sortKey.name())
                    ? Stream.of(SortBuilders.scoreSort().order(order))
                    : sortKey.jsonPaths()
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

    private void handleSearchAfter(SearchSourceBuilder builder) {
        var sortKeys = parameters().remove(keySearchAfter()).split(COMMA);
        if (nonNull(sortKeys)) {
            builder.searchAfter(sortKeys);
        }
    }

    private boolean fetchSource() {
        return nonNull(getExclude()) || nonNull(getInclude());
    }

    private boolean includeAggregation() {
        return getMediaType().is(JSON_UTF_8) && ALL.equalsIgnoreCase(parameters().get(keyAggregation()).as());
    }

    private boolean isMustNot(K key) {
        return NO_ITEMS.equals(key.searchOperator())
            || NOT_ONE_ITEM.equals(key.searchOperator());
    }
}