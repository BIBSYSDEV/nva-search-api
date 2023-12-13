package no.unit.nva.search2.common;

import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;
import static no.unit.nva.search2.common.QueryTools.decodeUTF;
import static no.unit.nva.search2.common.QueryTools.nextResultsBySortKey;
import static no.unit.nva.search2.common.QueryTools.splitValues;
import static no.unit.nva.search2.constant.ErrorMessages.UNEXPECTED_VALUE;
import static no.unit.nva.search2.constant.Functions.readSearchInfrastructureApiUri;
import static no.unit.nva.search2.constant.Patterns.PATTERN_IS_URL_PARAM_INDICATOR;
import static no.unit.nva.search2.constant.Words.COLON;
import static no.unit.nva.search2.constant.Words.COMMA;
import static no.unit.nva.search2.constant.Words.PLUS;
import static nva.commons.core.attempt.Try.attempt;
import com.google.common.net.MediaType;
import java.net.URI;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Stream;
import no.unit.nva.search.CsvTransformer;
import no.unit.nva.search2.constant.Words;
import no.unit.nva.search2.dto.PagedSearch;
import no.unit.nva.search2.dto.PagedSearchBuilder;
import no.unit.nva.search2.enums.ParameterKey;
import no.unit.nva.search2.enums.ParameterKey.ValueEncoding;
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

public abstract class Query<K extends Enum<K> & ParameterKey> {

    protected static final Logger logger = LoggerFactory.getLogger(Query.class);

    protected final transient Map<K, String> pageParameters;
    protected final transient Map<K, String> searchParameters;
    protected final transient Set<K> otherRequiredKeys;
    protected final transient QueryTools<K> queryTools;

    protected transient URI openSearchUri = URI.create(readSearchInfrastructureApiUri());
    private transient MediaType mediaType;
    private transient URI gatewayUri = URI.create("https://unset/resource/search");

    protected abstract Integer getFrom();

    protected abstract Integer getSize();

    protected abstract K getFieldsKey();

    protected abstract String[] fieldsToKeyNames(String field);

    public abstract String getSort();

    /**
     * Builds URI to query SWS based on post body.
     *
     * @return an URI to Sws search without parameters.
     */
    protected abstract URI getOpenSearchUri();

    protected Query() {
        searchParameters = new ConcurrentHashMap<>();
        pageParameters = new ConcurrentHashMap<>();
        otherRequiredKeys = new HashSet<>();
        queryTools = new QueryTools<>();
        mediaType = MediaType.JSON_UTF_8;
    }

    public <R, Q extends Query<K>> String doSearch(OpenSearchClient<R, Q> queryClient) {
        final var response = queryClient.doSearch((Q) this);
        return MediaType.CSV_UTF_8.is(this.getMediaType())
            ? toCsvText((SwsResponse) response)
            : toPagedResponse((SwsResponse) response).toJsonString();
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
            .forEach(entry -> results.put(toNvaSearchApiKey(entry), entry.getValue().replace(Words.SPACE, PLUS)));
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

    public Optional<String> getOptional(K key) {
        return Optional.ofNullable(
            searchParameters.containsKey(key)
                ? searchParameters.get(key)
                : pageParameters.get(key)
        );
    }

    public String removeKey(K key) {
        return searchParameters.containsKey(key)
            ? searchParameters.remove(key)
            : pageParameters.remove(key);
    }

    public boolean isPresent(K key) {
        return searchParameters.containsKey(key) || pageParameters.containsKey(key);
    }

    @JacocoGenerated
    public boolean hasNoSearchValue() {
        return searchParameters.isEmpty();
    }

    /**
     * Add a key value pair to searchable Parameters.
     *
     * @param key   to add to.
     * @param value to assign
     */
    public void setSearchingValue(K key, String value) {
        if (nonNull(value)) {
            var decodedValue = key.valueEncoding() != ValueEncoding.NONE ? decodeUTF(value) : value;
            searchParameters.put(key, decodedValue);
        }
    }

    /**
     * Add a key value pair to non-searchable Parameters.
     *
     * @param key   to add to.
     * @param value to assign
     */
    public void setPagingValue(K key, String value) {
        if (nonNull(value)) {
            pageParameters.put(key, key.valueEncoding() != ValueEncoding.NONE ? decodeUTF(value) : value);
        }
    }

    private MediaType getMediaType() {
        return mediaType;
    }

    public void setMediaType(String mediaType) {
        if (nonNull(mediaType) && mediaType.contains(Words.TEXT_CSV)) {
            this.mediaType = MediaType.CSV_UTF_8;
        } else {
            this.mediaType = MediaType.JSON_UTF_8;
        }
    }

    public URI getNvaSearchApiUri() {
        return gatewayUri;
    }

    public void setNvaSearchApiUri(URI gatewayUri) {
        this.gatewayUri = gatewayUri;
    }

    public void setOpenSearchUri(URI openSearchUri) {
        this.openSearchUri = openSearchUri;
    }

    protected String toNvaSearchApiKey(Entry<K, String> entry) {
        return entry.getKey().fieldName().toLowerCase(Locale.getDefault());
    }

    /**
     * Creates a boolean query, with all the search parameters.
     *
     * @return a BoolQueryBuilder
     */
    @SuppressWarnings({"PMD.SwitchStmtsShouldHaveDefault"})
    protected BoolQueryBuilder boolQuery() {
        var bq = QueryBuilders.boolQuery();
        getSearchParameterKeys()
            .flatMap(this::getQueryBuilders)
            .forEach(entry -> {
                switch (entry.getKey().searchOperator()) {
                    case MUST, GREATER_THAN_OR_EQUAL_TO, LESS_THAN -> bq.must(entry.getValue());
                    case MUST_NOT -> bq.mustNot(entry.getValue());
                    case SHOULD -> bq.should(entry.getValue());
                    default -> throw new IllegalStateException(UNEXPECTED_VALUE + entry.getKey().searchOperator());
                }
            });
        return bq;
    }

    private Stream<Entry<K, QueryBuilder>> getQueryBuilders(K key) {
        final var values = splitValues(searchParameters.get(key));
        if (queryTools.isSearchAll(key)) {
            return queryTools.queryToEntry(key, multiMatchQuery(key, getFieldsKey()));
        } else if (queryTools.isFundingKey(key)) {
            return queryTools.fundingQuery(key, searchParameters.get(key));
        } else if (queryTools.isNumber(key)) {
            return queryTools.rangeQuery(key, values[0]); //assumes one value, can be extended -> 'FROM X TO Y'
        } else {
            return queryTools.buildQuery(key, values)
                .flatMap(builder -> queryTools.queryToEntry(key, builder));
        }
    }

    private Stream<K> getSearchParameterKeys() {
        return searchParameters.keySet().stream();
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
            .type(MultiMatchQueryBuilder.Type.CROSS_FIELDS)
            .operator(Operator.AND);
    }

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
            return (T) switch (key.fieldType()) {
                case DATE -> castDateTime();
                case NUMBER -> castNumber();
                default -> value;
            };
        }

        public K getKey() {
            return key;
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

    // SORTING
    protected Stream<Entry<String, SortOrder>> getSortStream() {
        var optionalSort = Optional.ofNullable(getSort());

        return optionalSort.isEmpty()
            ? Stream.of()
            : Arrays.stream(optionalSort.get().split(COMMA))
                .map(sort -> sort.split(COLON))
                .map(QueryTools::stringsToEntry)
                .map(QueryTools::entryToSortEntry);
    }
}
