package no.unit.nva.search.common;

import static com.google.common.net.MediaType.CSV_UTF_8;
import static com.google.common.net.MediaType.JSON_UTF_8;

import static no.unit.nva.constants.Defaults.DEFAULT_SORT_ORDER;
import static no.unit.nva.constants.Defaults.ZERO_RESULTS_AGGREGATION_ONLY;
import static no.unit.nva.constants.Words.ALL;
import static no.unit.nva.constants.Words.ASTERISK;
import static no.unit.nva.constants.Words.COMMA;
import static no.unit.nva.constants.Words.KEYWORD_FALSE;
import static no.unit.nva.constants.Words.POST_FILTER;
import static no.unit.nva.constants.Words.RELEVANCE_KEY_NAME;
import static no.unit.nva.constants.Words.SORT_LAST;
import static no.unit.nva.search.common.constant.Patterns.COLON_OR_SPACE;
import static no.unit.nva.search.common.enums.FieldOperator.NOT_ALL_OF;
import static no.unit.nva.search.common.enums.FieldOperator.NOT_ANY_OF;

import static nva.commons.core.attempt.Try.attempt;

import static java.util.Objects.nonNull;

import com.google.common.net.MediaType;

import no.unit.nva.constants.ErrorMessages;
import no.unit.nva.constants.Words;
import no.unit.nva.search.common.builder.AcrossFieldsQuery;
import no.unit.nva.search.common.builder.ExistsQuery;
import no.unit.nva.search.common.builder.FuzzyKeywordQuery;
import no.unit.nva.search.common.builder.HasPartsQuery;
import no.unit.nva.search.common.builder.KeywordQuery;
import no.unit.nva.search.common.builder.PartOfQuery;
import no.unit.nva.search.common.builder.RangeQuery;
import no.unit.nva.search.common.builder.TextQuery;
import no.unit.nva.search.common.constant.Functions;
import no.unit.nva.search.common.enums.ParameterKey;
import no.unit.nva.search.common.enums.SortKey;
import no.unit.nva.search.common.records.HttpResponseFormatter;
import no.unit.nva.search.common.records.QueryContentWrapper;
import no.unit.nva.search.common.records.SwsResponse;

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

import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * SearchQuery is a class that represents a query to the search service.
 *
 * @author Stig Norland
 */
@SuppressWarnings("PMD.GodClass")
public abstract class SearchQuery<K extends Enum<K> & ParameterKey<K>> extends Query<K> {

    protected static final Logger logger = LoggerFactory.getLogger(SearchQuery.class);
    public final transient QueryFilter filters;
    private transient MediaType mediaType;

    /**
     * Always set at runtime by ParameterValidator.fromRequestInfo(RequestInfo requestInfo); This
     * value only used in debug and tests.
     */
    private transient URI gatewayUri =
            URI.create("https://api.dev.nva.aws.unit.no/resource/search");

    protected SearchQuery() {
        super();
        filters = new QueryFilter();
        queryKeys = new QueryKeys<>(keyFields());
        setMediaType(JSON_UTF_8.toString());
    }

    protected abstract AsType<K> from();

    protected abstract AsType<K> size();

    protected abstract AsType<K> sort();

    protected abstract String[] exclude();

    protected abstract String[] include();

    protected abstract K keyAggregation();

    protected abstract K keyFields();

    protected abstract K keySearchAfter();

    protected abstract K toKey(String keyName);

    protected abstract SortKey toSortKey(String sortName);

    /**
     * Path to each and every facet defined in builderAggregations().
     *
     * @return MapOf(Name, Path)
     */
    protected abstract Map<String, String> facetPaths();

    protected abstract List<AggregationBuilder> builderAggregations();

    protected abstract Stream<Entry<K, QueryBuilder>> builderCustomQueryStream(K key);

    @Override
    public <R, Q extends Query<K>> HttpResponseFormatter<K> doSearch(
            OpenSearchClient<R, Q> queryClient) {
        return new HttpResponseFormatter<>(
                (SwsResponse) queryClient.doSearch((Q) this),
                getMediaType(),
                getNvaSearchApiUri(),
                from().as(),
                size().as(),
                facetPaths(),
                parameters());
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

    public URI getNvaSearchApiUri() {
        return gatewayUri;
    }

    @JacocoGenerated
    public void setNvaSearchApiUri(URI gatewayUri) {
        this.gatewayUri = gatewayUri;
    }

    protected void setOpenSearchUri(URI openSearchUri) {
        this.infrastructureApiUri = openSearchUri;
    }

    /**
     * Creates a multi match query, all words needs to be present, within a document.
     *
     * @return a MultiMatchQueryBuilder
     */
    protected Map<String, Float> mapOfPathAndBoost(AsType<K> fieldValue) {
        return fieldValue.isEmpty() || fieldValue.asLowerCase().contains(ALL)
                ? Map.of(ASTERISK, 1F) // NONE or ALL -> <'*',1.0>
                : fieldValue
                        .asSplitStream(COMMA)
                        .map(this::toKey)
                        .flatMap(this::entryStreamOfPathAndBoost)
                        .collect(Collectors.toMap(Entry::getKey, Entry::getValue));
    }

    @JacocoGenerated // default can only be tested if we add a new fieldtype not in use....
    protected Stream<Entry<K, QueryBuilder>> builderStreamDefaultQuery(K key) {
        final var value = parameters().get(key).toString();
        return switch (key.fieldType()) {
            case DATE, NUMBER -> new RangeQuery<K>().buildQuery(key, value);
            case KEYWORD -> new KeywordQuery<K>().buildQuery(key, value);
            case FUZZY_KEYWORD -> new FuzzyKeywordQuery<K>().buildQuery(key, value);
            case TEXT -> new TextQuery<K>().buildQuery(key, value);
            case FREE_TEXT -> Functions.queryToEntry(key, builderSearchAllQuery(key));
            case ACROSS_FIELDS -> new AcrossFieldsQuery<K>().buildQuery(key, value);
            case EXISTS -> new ExistsQuery<K>().buildQuery(key, value);
            case HAS_PARTS -> new HasPartsQuery<K>().buildQuery(key, value);
            case PART_OF -> new PartOfQuery<K>().buildQuery(key, value);
            case CUSTOM -> builderCustomQueryStream(key);
            case FLAG -> Stream.empty();
            default -> throw new RuntimeException(ErrorMessages.HANDLER_NOT_DEFINED + key.name());
        };
    }

    @Override
    public Stream<QueryContentWrapper> assemble() {
        // TODO extract builderDefaultSearchSource() and this content into a separate class?
        var contentWrappers = new ArrayList<QueryContentWrapper>(numberOfRequests());
        var builder = builderDefaultSearchSource();

        handleFetchSource(builder);
        handleAggregation(builder, contentWrappers);
        handleSearchAfter(builder);
        handleSorting(builder);
        contentWrappers.add(new QueryContentWrapper(builder.toString(), this.openSearchUri()));
        return contentWrappers.stream();
    }

    /**
     * Creates a boolean query, with all the search parameters.
     *
     * @return a BoolQueryBuilder
     */
    protected BoolQueryBuilder builderMainQuery() {
        var boolQueryBuilder = QueryBuilders.boolQuery();
        parameters()
                .getSearchKeys()
                .flatMap(this::builderStreamDefaultQuery)
                .forEach(
                        entry -> {
                            if (isMustNot(entry.getKey())) {
                                boolQueryBuilder.mustNot(entry.getValue());
                            } else {
                                boolQueryBuilder.must(entry.getValue());
                            }
                        });
        return boolQueryBuilder;
    }

    protected Stream<SortBuilder<?>> builderStreamFieldSort() {
        return sort().asSplitStream(COMMA)
                .flatMap(
                        item -> {
                            final var parts = item.split(COLON_OR_SPACE);
                            final var order =
                                    SortOrder.fromString(
                                            attempt(() -> parts[1])
                                                    .orElse((f) -> DEFAULT_SORT_ORDER));
                            final var sortKey = toSortKey(parts[0]);

                            return RELEVANCE_KEY_NAME.equalsIgnoreCase(sortKey.name())
                                    ? Stream.of(SortBuilders.scoreSort().order(order))
                                    : sortKey.jsonPaths()
                                            .map(
                                                    path ->
                                                            SortBuilders.fieldSort(path)
                                                                    .order(order)
                                                                    .missing(SORT_LAST));
                        });
    }

    protected AggregationBuilder builderAggregationsWithFilter() {
        var aggrFilter = AggregationBuilders.filter(POST_FILTER, filters.get());
        builderAggregations().forEach(aggrFilter::subAggregation);
        return aggrFilter;
    }

    protected SearchSourceBuilder builderDefaultSearchSource() {
        var queryBuilder =
                parameters().getSearchKeys().findAny().isEmpty()
                        ? QueryBuilders.matchAllQuery()
                        : builderMainQuery();

        return new SearchSourceBuilder()
                .query(queryBuilder)
                .size(size().as())
                .from(from().as())
                .postFilter(filters.get())
                .trackTotalHits(true);
    }

    protected QueryBuilder builderSearchAllQuery(K searchAllKey) {
        var fields = mapOfPathAndBoost(parameters().get(keyFields()));
        var value = parameters().get(searchAllKey).toString();
        return QueryBuilders.multiMatchQuery(value)
                .fields(fields)
                .type(Type.CROSS_FIELDS)
                .operator(Operator.AND);
    }

    private void handleAggregation(
            SearchSourceBuilder builder, List<QueryContentWrapper> contentWrappers) {
        if (hasAggregation()) {
            var aggregationBuilder = builder.shallowCopy();
            aggregationBuilder.size(ZERO_RESULTS_AGGREGATION_ONLY);
            aggregationBuilder.aggregation(builderAggregationsWithFilter());
            contentWrappers.add(
                    new QueryContentWrapper(aggregationBuilder.toString(), this.openSearchUri()));
        }
    }

    private void handleFetchSource(SearchSourceBuilder builder) {
        if (isFetchSource()) {
            builder.fetchSource(include(), exclude());
        } else {
            builder.fetchSource(true);
        }
    }

    private void handleSearchAfter(SearchSourceBuilder builder) {
        var sortKeys = parameters().remove(keySearchAfter()).split(COMMA);
        if (nonNull(sortKeys)) {
            builder.searchAfter(sortKeys);
        }
    }

    private void handleSorting(SearchSourceBuilder builder) {
        if (isSortByRelevance()) {
            builder.trackScores(
                    true); // Not very well documented. This allows sorting on relevance and other
            // fields.
        }
        builderStreamFieldSort().forEach(builder::sort);
    }

    private Stream<Entry<String, Float>> entryStreamOfPathAndBoost(K key) {
        return key.searchFields(KEYWORD_FALSE).map(jsonPath -> entryOfPathAndBoost(key, jsonPath));
    }

    private Entry<String, Float> entryOfPathAndBoost(K key, String jsonPath) {
        return Map.entry(jsonPath, key.fieldBoost());
    }

    private int numberOfRequests() {
        return hasAggregation() ? 2 : 1;
    }

    private boolean isSortByRelevance() {
        var sorts = sort().toString();
        return nonNull(sorts)
                && sorts.split(COMMA).length > 1
                && sorts.contains(RELEVANCE_KEY_NAME);
    }

    private boolean isMustNot(K key) {
        return NOT_ALL_OF.equals(key.searchOperator()) || NOT_ANY_OF.equals(key.searchOperator());
    }

    private boolean isFetchSource() {
        return nonNull(exclude()) || nonNull(include());
    }

    private boolean hasAggregation() {
        return getMediaType().is(JSON_UTF_8)
                && ALL.equalsIgnoreCase(parameters().get(keyAggregation()).as());
    }
}
