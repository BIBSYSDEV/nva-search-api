package no.unit.nva.search2.common;

import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;
import static no.unit.nva.search2.common.QueryTools.decodeUTF;
import static no.unit.nva.search2.common.QueryTools.hasContent;
import static no.unit.nva.search2.common.constant.Functions.readSearchInfrastructureApiUri;
import static no.unit.nva.search2.common.constant.Patterns.PATTERN_IS_URL_PARAM_INDICATOR;
import static no.unit.nva.search2.common.constant.Words.ALL;
import static no.unit.nva.search2.common.constant.Words.ASTERISK;
import static no.unit.nva.search2.common.constant.Words.COMMA;
import static no.unit.nva.search2.common.constant.Words.PLUS;
import static no.unit.nva.search2.common.constant.Words.SPACE;
import static no.unit.nva.search2.common.enums.FieldOperator.NOT_ONE_ITEM;
import static no.unit.nva.search2.common.enums.FieldOperator.NO_ITEMS;
import static nva.commons.core.paths.UriWrapper.fromUri;
import com.google.common.net.MediaType;
import java.net.URI;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import no.unit.nva.search.CsvTransformer;
import no.unit.nva.search2.common.builder.OpensearchQueryFuzzyKeyword;
import no.unit.nva.search2.common.builder.OpensearchQueryKeyword;
import no.unit.nva.search2.common.builder.OpensearchQueryRange;
import no.unit.nva.search2.common.builder.OpensearchQueryText;
import no.unit.nva.search2.common.constant.Words;
import no.unit.nva.search2.common.enums.ParameterKey;
import no.unit.nva.search2.common.enums.ValueEncoding;
import no.unit.nva.search2.common.records.PagedSearch;
import no.unit.nva.search2.common.records.PagedSearchBuilder;
import no.unit.nva.search2.common.records.SwsResponse;
import nva.commons.core.JacocoGenerated;
import org.opensearch.index.query.BoolQueryBuilder;
import org.opensearch.index.query.MultiMatchQueryBuilder.Type;
import org.opensearch.index.query.Operator;
import org.opensearch.index.query.QueryBuilder;
import org.opensearch.index.query.QueryBuilders;
import org.opensearch.search.builder.SearchSourceBuilder;
import org.opensearch.search.sort.SortOrder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@SuppressWarnings("PMD.GodClass")
public abstract class Query<K extends Enum<K> & ParameterKey> {

    protected static final Logger logger = LoggerFactory.getLogger(Query.class);

    protected final transient Map<K, String> pageParameters;
    protected final transient Map<K, String> searchParameters;
    protected final transient Set<K> otherRequiredKeys;
    protected final transient QueryTools<K> queryTools;
    protected transient URI openSearchUri = URI.create(readSearchInfrastructureApiUri());
    private final transient List<QueryBuilder> filters = new ArrayList<>();
    private transient MediaType mediaType;
    private transient URI gatewayUri = URI.create("https://unset/resource/search");

    public abstract AsType<K> getSort();

    protected abstract Integer getFrom();

    protected abstract Integer getSize();

    protected abstract K getFieldsKey();

    /**
     * Builds URI to query SWS based on post body.
     *
     * @return an URI to Sws search without parameters.
     */
    protected abstract URI getOpenSearchUri();

    protected abstract boolean isPagingValue(K key);

    protected abstract Map<String, String> aggregationsDefinition();

    @JacocoGenerated    // default value shouldn't happen, (developer have forgotten to handle a key)
    protected abstract Stream<Entry<K, QueryBuilder>> customQueryBuilders(K key);

    protected abstract K keyFromString(String keyName);

    protected Query() {
        searchParameters = new ConcurrentHashMap<>();
        pageParameters = new ConcurrentHashMap<>();
        otherRequiredKeys = new HashSet<>();
        queryTools = new QueryTools<>();

        setMediaType(MediaType.JSON_UTF_8.toString());
    }

    public <R, Q extends Query<K>> String doSearch(OpenSearchClient<R, Q> queryClient) {
        logSearchKeys();
        final var response = (SwsResponse) queryClient.doSearch((Q) this);
        return MediaType.CSV_UTF_8.is(this.getMediaType())
            ? toCsvText(response)
            : toPagedResponse(response).toJsonString();
    }

    protected String toCsvText(SwsResponse response) {
        return CsvTransformer.transform(response.getSearchHits());
    }

    public PagedSearch toPagedResponse(SwsResponse response) {
        final var requestParameter = toNvaSearchApiRequestParameter();
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

    /**
     * Query Parameters with string Keys.
     *
     * @return Map of String and String
     */
    public Map<String, String> toNvaSearchApiRequestParameter() {
        var results = new LinkedHashMap<String, String>();
        Stream.of(searchParameters.entrySet(), pageParameters.entrySet())
            .flatMap(Set::stream)
            .sorted(Comparator.comparingInt(o -> o.getKey().ordinal()))
            .forEach(entry -> results.put(toNvaSearchApiKey(entry), toNvaSearchApiValue(entry)));
        return results;
    }

    /**
     * Get value from Query Parameter Map with key.
     *
     * @param key to look up.
     * @return String content raw
     */
    public AsType<K> getValue(K key) {
        return new AsType<>(
            searchParameters.containsKey(key)
                ? searchParameters.get(key)
                : pageParameters.get(key),
            key
        );
    }

    /**
     * Add a key value pair to Parameters.
     *
     * @param key   to add to.
     * @param value to assign
     */
    public void setKeyValue(K key, String value) {
        if (nonNull(value)) {
            var decodedValue = key.valueEncoding() != ValueEncoding.NONE
                ? decodeUTF(value)
                : value;
            if (isPagingValue(key)) {
                pageParameters.put(key, decodedValue);
            } else {
                searchParameters.put(key, decodedValue);
            }
        }
    }

    public AsType<K> removeKey(K key) {
        return new AsType<>(
            searchParameters.containsKey(key)
            ? searchParameters.remove(key)
                : pageParameters.remove(key),
            key
        );
    }

    public boolean isPresent(K key) {
        return searchParameters.containsKey(key) || pageParameters.containsKey(key);
    }

    protected boolean hasOneValue(K key) {
        return getValue(key)
            .asStream()
            .anyMatch(p -> !p.contains(COMMA));
    }

    protected boolean hasNoSearchValue() {
        return searchParameters.isEmpty();
    }

    protected MediaType getMediaType() {
        return mediaType;
    }

    final void setMediaType(String mediaType) {
        if (nonNull(mediaType) && mediaType.contains(Words.TEXT_CSV)) {
            this.mediaType = MediaType.CSV_UTF_8;
        } else {
            this.mediaType = MediaType.JSON_UTF_8;
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

    protected Map<String, Float> fieldsToKeyNames(String field) {
        return ALL.equals(field) || isNull(field)
            ? Map.of(ASTERISK, 1F)       // NONE or ALL -> ['*']
            : Arrays.stream(field.split(COMMA))
                .map(this::keyFromString)
                .flatMap(key -> key.searchFields()
                    .map(jsonPath -> Map.entry(jsonPath, key.fieldBoost()))
                )
                .collect(Collectors.toMap(Entry::getKey, Entry::getValue));
    }

    protected BoolQueryBuilder getFilters() {
        var boolQueryBuilder = QueryBuilders.boolQuery();
        filters.forEach(boolQueryBuilder::must);
        return boolQueryBuilder;
    }

    protected void setFilters(QueryBuilder... filters) {
        this.filters.clear();
        this.filters.addAll(List.of(filters));
    }

    protected void addFilter(QueryBuilder builder) {
        this.filters.removeIf(filter -> filter.getName().equals(builder.getName()));
        this.filters.add(builder);
    }

    protected String toNvaSearchApiKey(Entry<K, String> entry) {
        return entry.getKey().fieldName().toLowerCase(Locale.getDefault());
    }

    protected String toNvaSearchApiValue(Entry<K, String> entry) {
        return entry.getValue().replace(SPACE, PLUS);
    }

    /**
     * Creates a boolean query, with all the search parameters.
     *
     * @return a BoolQueryBuilder
     */
    protected BoolQueryBuilder mainQuery() {
        var boolQueryBuilder = QueryBuilders.boolQuery();
        searchParameters.keySet().stream()
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
            .postFilter(getFilters())
            .trackTotalHits(true);
    }


    // SORTING
    protected Stream<Entry<String, SortOrder>> getSortStream() {
        return getSort().asStream()
            .map(items -> items.split(COMMA))
            .flatMap(Arrays::stream)
            .map(QueryTools::objectToSortEntry);
    }


    private Stream<Entry<K, QueryBuilder>> getQueryBuilders(K key) {
        final var value = searchParameters.get(key);
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

    /**
     * Creates a multi match query, all words needs to be present, within a document.
     *
     * @return a MultiMatchQueryBuilder
     */
    private QueryBuilder multiMatchQuery(K searchAllKey, K fieldsKey) {
        var fields = fieldsToKeyNames(getValue(fieldsKey).toString());
        var value = getValue(searchAllKey).toString();
        logger.info(value);
        return QueryBuilders.multiMatchQuery(value)
            .fields(fields)
            .type(Type.CROSS_FIELDS)
            .operator(Operator.AND);
    }

    private Stream<K> getSearchParameterKeys() {
        return searchParameters.keySet().stream();
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
            getSearchParameterKeys()
                .map(Enum::name)
                .collect(Collectors.joining("\", \"", "{\"keys\":[\"", "\"]}"))
        );
    }

    private boolean isMustNot(K key) {
        return NO_ITEMS.equals(key.searchOperator())
               || NOT_ONE_ITEM.equals(key.searchOperator());
    }
}
