package no.unit.nva.search2.common;

import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;
import static no.unit.nva.search2.common.QueryTools.decodeUTF;
import static no.unit.nva.search2.common.QueryTools.hasContent;
import static no.unit.nva.search2.common.constant.Functions.jsonPath;
import static no.unit.nva.search2.common.constant.Functions.readSearchInfrastructureApiUri;
import static no.unit.nva.search2.common.constant.Patterns.PATTERN_IS_URL_PARAM_INDICATOR;
import static no.unit.nva.search2.common.constant.Words.AFFILIATIONS;
import static no.unit.nva.search2.common.constant.Words.COMMA;
import static no.unit.nva.search2.common.constant.Words.CONTRIBUTORS;
import static no.unit.nva.search2.common.constant.Words.CONTRIBUTORS_PART_OFS;
import static no.unit.nva.search2.common.constant.Words.CRISTIN_SOURCE;
import static no.unit.nva.search2.common.constant.Words.ENTITY_DESCRIPTION;
import static no.unit.nva.search2.common.constant.Words.EXCLUDE_SUBUNITS;
import static no.unit.nva.search2.common.constant.Words.ID;
import static no.unit.nva.search2.common.constant.Words.KEYWORD;
import static no.unit.nva.search2.common.constant.Words.PLUS;
import static no.unit.nva.search2.common.constant.Words.SCOPUS_SOURCE;
import static no.unit.nva.search2.common.constant.Words.SPACE;
import static no.unit.nva.search2.common.constant.Words.VIEWING_SCOPE;
import static no.unit.nva.search2.common.enums.FieldOperator.NOT_ONE_ITEM;
import static no.unit.nva.search2.common.enums.FieldOperator.NO_ITEMS;
import static nva.commons.core.attempt.Try.attempt;
import static nva.commons.core.paths.UriWrapper.fromUri;
import com.google.common.net.MediaType;
import java.net.URI;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
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
import org.joda.time.DateTime;
import org.opensearch.index.query.BoolQueryBuilder;
import org.opensearch.index.query.MultiMatchQueryBuilder;
import org.opensearch.index.query.Operator;
import org.opensearch.index.query.QueryBuilder;
import org.opensearch.index.query.QueryBuilders;
import org.opensearch.search.sort.SortOrder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@SuppressWarnings("PMD.GodClass")
public abstract class Query<K extends Enum<K> & ParameterKey> {

    protected static final Logger logger = LoggerFactory.getLogger(Query.class);

    protected final transient Map<K, String> pageParameters;
    protected final transient Map<K, String> searchParameters;
    protected final transient Set<K> otherRequiredKeys;
    protected final transient QueryTools<K> opensearchQueryTools;
    protected transient URI openSearchUri = URI.create(readSearchInfrastructureApiUri());
    private final transient List<QueryBuilder> filters = new ArrayList<>();
    private transient MediaType mediaType;
    private transient URI gatewayUri = URI.create("https://unset/resource/search");

    public abstract AsType getSort();

    protected abstract Integer getFrom();

    protected abstract Integer getSize();

    protected abstract K getFieldsKey();

    protected abstract String[] fieldsToKeyNames(String field);

    /**
     * Builds URI to query SWS based on post body.
     *
     * @return an URI to Sws search without parameters.
     */
    protected abstract URI getOpenSearchUri();

    protected abstract boolean isPagingValue(K key);

    protected Query() {
        searchParameters = new ConcurrentHashMap<>();
        pageParameters = new ConcurrentHashMap<>();
        otherRequiredKeys = new HashSet<>();
        opensearchQueryTools = new QueryTools<>();

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

        return
            new PagedSearchBuilder()
                .withTotalHits(response.getTotalSize())
                .withHits(response.getSearchHits())
                .withIds(source, requestParameter, getFrom(), getSize())
                .withNextResultsBySortKey(nextResultsBySortKey(response, requestParameter, source))
                .withAggregations(response.getAggregationsStructured())
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
    public AsType getValue(K key) {
        return new AsType(
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

    public String removeKey(K key) {
        return searchParameters.containsKey(key)
            ? searchParameters.remove(key)
            : pageParameters.remove(key);
    }

    public boolean isPresent(K key) {
        return searchParameters.containsKey(key) || pageParameters.containsKey(key);
    }

    protected boolean hasOneValue(K key) {
        return getValue(key)
            .optionalStream()
            .anyMatch(p -> !p.contains(COMMA));
    }

    protected boolean hasNoSearchValue() {
        return searchParameters.isEmpty();
    }

    private MediaType getMediaType() {
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
    @SuppressWarnings({"PMD.SwitchStmtsShouldHaveDefault"})
    protected BoolQueryBuilder boolQuery() {
        var boolQueryBuilder = QueryBuilders.boolQuery();
        var searchParameterKeys = getSearchParameterKeys().toList();
        searchParameterKeys.stream()
            .flatMap(this::getQueryBuilders)
            .forEach(entry -> {
                if (isMustNot(entry.getKey())) {
                    boolQueryBuilder.mustNot(entry.getValue());
                } else {
                    boolQueryBuilder.must(entry.getValue());
                }
            });
        addExcludeSubunitsQuery(boolQueryBuilder, searchParameterKeys);
        return boolQueryBuilder;
    }

    private void addExcludeSubunitsQuery(BoolQueryBuilder boolQueryBuilder, List<K> searchParameterKeys) {
        var viewingScope = getViewingScope(searchParameterKeys);
        if (viewingScopeIsProvided(viewingScope)) {
            var shouldExcludeSubunits = getExcludeSubunitsKey(searchParameterKeys);
            boolQueryBuilder.must(createSubunitQuery(shouldExcludeSubunits, viewingScope));
        }
    }

    private static QueryBuilder createSubunitQuery(Boolean shouldExcludeSubunits, List<String> viewingScope) {
        return shouldExcludeSubunits ? excludeSubunitsQuery(viewingScope) : includeSubunitsQuery(viewingScope);
    }

    private static boolean viewingScopeIsProvided(List<String> viewingScope) {
        return !viewingScope.isEmpty();
    }

    private List<String> getViewingScope(List<K> searchParameterKeys) {
        var viewingScopeParameter = getViewingScopeParameter(searchParameterKeys);
        return nonNull(viewingScopeParameter)
                   ? extractViewingScope(viewingScopeParameter)
                   : List.of();
    }

    private String getViewingScopeParameter(List<K> searchParameterKeys) {
        return searchParameterKeys.stream()
                   .filter(key -> VIEWING_SCOPE.equals(key.name()))
                   .map(searchParameters::get)
                   .findFirst().orElse(null);
    }

    private static List<String> extractViewingScope(String viewingScopeParameter) {
        return Arrays.stream(viewingScopeParameter.split(COMMA))
                   .map(value -> URLDecoder.decode(value, StandardCharsets.UTF_8))
                   .toList();
    }

    private Boolean getExcludeSubunitsKey(List<K> searchParameterKeys) {
        return searchParameterKeys.stream()
                   .filter(key -> EXCLUDE_SUBUNITS.equals(key.name()))
                   .findFirst()
                   .map(searchParameters::get)
                   .map(Boolean::parseBoolean)
                   .orElse(false);
    }

    private static QueryBuilder includeSubunitsQuery(List<String> viewingScope) {
        var query = QueryBuilders.boolQuery();
        query.should(QueryBuilders.termsQuery(jsonPath(CONTRIBUTORS_PART_OFS, KEYWORD), viewingScope));
        query.should(QueryBuilders.termsQuery(jsonPath(ENTITY_DESCRIPTION, CONTRIBUTORS, AFFILIATIONS, ID, KEYWORD),
                                              viewingScope));
        return query;
    }

    private static QueryBuilder excludeSubunitsQuery(List<String> viewingScope) {
        return QueryBuilders.termsQuery(jsonPath(ENTITY_DESCRIPTION, CONTRIBUTORS, AFFILIATIONS, ID, KEYWORD),
                                        viewingScope);
    }

    // SORTING
    protected Stream<Entry<String, SortOrder>> getSortStream() {
        return getSort().optionalStream()
            .map(items -> items.split(COMMA))
            .flatMap(Arrays::stream)
            .map(QueryTools::objectToSortEntry);
    }

    private boolean isMustNot(K key) {
        return NO_ITEMS.equals(key.searchOperator())
               || NOT_ONE_ITEM.equals(key.searchOperator());
    }

    private Stream<Entry<K, QueryBuilder>> getQueryBuilders(K key) {
        final var value = searchParameters.get(key);
        if (opensearchQueryTools.isSearchAllKey(key)) {
            return opensearchQueryTools.queryToEntry(key, multiMatchQuery(key, getFieldsKey()));
            // -> E M P T Y  S P A C E
        } else if (opensearchQueryTools.isFundingKey(key)) {
            return opensearchQueryTools.fundingQuery(key, value);
            // -> E M P T Y  S P A C E
        } else if (opensearchQueryTools.isCristinIdentifierKey(key)) {
            return opensearchQueryTools.additionalIdentifierQuery(key, value, CRISTIN_SOURCE);
            // -> E M P T Y  S P A C E
        } else if (opensearchQueryTools.isScopusIdentifierKey(key)) {
            return opensearchQueryTools.additionalIdentifierQuery(key, value, SCOPUS_SOURCE);
            // -> E M P T Y  S P A C E
        } else if (opensearchQueryTools.isPublicFile(key)) {
            return opensearchQueryTools.publishedFileQuery(key, value);
            // -> E M P T Y  S P A C E
        } else if (opensearchQueryTools.isBooleanKey(key)) {
            return opensearchQueryTools.boolQuery(key, value); //TODO make validation pattern... (assumes one value)
            // -> E M P T Y  S P A C E
        } else if (opensearchQueryTools.isNumberKey(key)) {
            return new OpensearchQueryRange<K>().buildQuery(key, value);
            // -> E M P T Y  S P A C E
        } else if (opensearchQueryTools.isTextKey(key)) {
            return new OpensearchQueryText<K>().buildQuery(key, value);
            // -> E M P T Y  S P A C E
        } else if (opensearchQueryTools.isFuzzyKeywordKey(key)) {
            return new OpensearchQueryFuzzyKeyword<K>().buildQuery(key, value);
            // -> E M P T Y  S P A C E
        } else if (opensearchQueryTools.isPublicFile(key)) {
            return opensearchQueryTools.publishedFileQuery(key, value);
            // -> E M P T Y  S P A C E
        } else if (opensearchQueryTools.isExcludeSubunits(key) || opensearchQueryTools.isViewingScope(key)) {
            return null;
            // -> E M P T Y  S P A C E
        } else {
            return new OpensearchQueryKeyword<K>().buildQuery(key, value);
        }
    }

    /**
     * Creates a multi match query, all words needs to be present, within a document.
     *
     * @return a MultiMatchQueryBuilder
     */
    private MultiMatchQueryBuilder multiMatchQuery(K searchAllKey, K fieldsKey) {
        var fields = fieldsToKeyNames(getValue(fieldsKey).toString());
        var value = getValue(searchAllKey).toString();

        return QueryBuilders
            .multiMatchQuery(value, fields)
            .type(MultiMatchQueryBuilder.Type.BEST_FIELDS)
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

    /**
     * AutoConvert value to Date, Number (or String)
     * <p>Also holds key and can return value as <samp>optional stream</samp></p>
     */
    @SuppressWarnings({"PMD.ShortMethodName"})
    public class AsType {

        private final String value;
        private final K key;

        public AsType(String value, K key) {
            this.value = value;
            this.key = key;
        }

        public <T> T as() {
            if (isNull(value)) {
                return null;
            }
            return (T) switch (getKey().fieldType()) {
                case DATE -> castDateTime();
                case NUMBER -> castNumber();
                default -> value;
            };
        }

        public K getKey() {
            return key;
        }

        public Stream<String> optionalStream() {
            return Optional.ofNullable(value).stream();
        }

        @Override
        public String toString() {
            return value;
        }

        private <T> T castDateTime() {
            return ((Class<T>) DateTime.class).cast(DateTime.parse(value));
        }

        private <T extends Number> T castNumber() {
            return (T) attempt(() -> Integer.parseInt(value)).orElseThrow();
        }
    }
}
