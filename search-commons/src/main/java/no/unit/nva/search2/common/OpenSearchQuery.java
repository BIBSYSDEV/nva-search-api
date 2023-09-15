package no.unit.nva.search2.common;

import static java.util.Objects.nonNull;
import static no.unit.nva.search2.constant.ApplicationConstants.EQUAL;
import static no.unit.nva.search2.constant.ApplicationConstants.RESOURCES;
import static no.unit.nva.search2.constant.ApplicationConstants.SEARCH;
import static no.unit.nva.search2.constant.ApplicationConstants.AND;
import static no.unit.nva.search2.constant.ApplicationConstants.AMPERSAND;
import static no.unit.nva.search2.constant.ApplicationConstants.PLUS;
import static no.unit.nva.search2.constant.ApplicationConstants.OR;
import static no.unit.nva.search2.constant.ApplicationConstants.PREFIX;
import static no.unit.nva.search2.constant.ApplicationConstants.SUFFIX;

import static no.unit.nva.search2.constant.ApplicationConstants.readSearchInfrastructureApiUri;
import static nva.commons.core.StringUtils.EMPTY_STRING;
import static nva.commons.core.attempt.Try.attempt;
import static nva.commons.core.paths.UriWrapper.fromUri;

import java.net.URI;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

import java.util.stream.Stream;
import no.unit.nva.search2.model.ParameterKey;
import no.unit.nva.search2.model.ParameterKey.KeyEncoding;
import nva.commons.apigateway.exceptions.ApiGatewayException;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public abstract class OpenSearchQuery<K extends Enum<K> & ParameterKey, R extends Record> {

    protected static final Logger logger = LoggerFactory.getLogger(OpenSearchQuery.class);
    protected static final String SPACE_ENCODED = "%20";
    protected final transient Map<K, String> queryParameters;
    protected final transient Map<K, String> luceneParameters;
    protected final transient Set<K> otherRequiredKeys;
    protected transient URI gatewayUri;

    protected OpenSearchQuery() {
        luceneParameters = new ConcurrentHashMap<>();
        queryParameters = new ConcurrentHashMap<>();
        otherRequiredKeys = new HashSet<>();
    }

    /**
     * Builds URI to query SWS based on parameters supplied to the builder methods.
     *
     * @return an URI to NVA (default) Projects with parameters.
     */
    public URI openSearchUri() {
        return
            fromUri(readSearchInfrastructureApiUri())
                .addChild(RESOURCES, SEARCH)
                .addQueryParameters(toLuceneParameter())
                .addQueryParameters(toParameters())
                .getUri();
    }

    /**
     * Query Parameters with string Keys.
     *
     * @return Map of String and String
     */
    public Map<String, String> toParameters() {
        var results =
            queryParameters.entrySet().stream()
                .collect(Collectors.toMap(this::toQueryName, this::toQueryValue));
        return new TreeMap<>(results);
    }

    /**
     * Query Parameters with string Keys.
     *
     * @return Map of String and String
     */
    public Map<String, String> toLuceneParameter() {
        var query = luceneParameters.entrySet().stream()
            .map(this::toLuceneEntryToString)
            .collect(Collectors.joining(AND));
        return Map.of("q", query);
    }

    /**
     * Query Parameters with string Keys.
     *
     * @return Map of String and String
     */
    public Map<String, String> toGateWayRequestParameter() {
        var results = new LinkedHashMap<String, String>();
        Stream.of(luceneParameters.entrySet(), queryParameters.entrySet())
            .flatMap(Set::stream)
            .sorted(Comparator.comparingInt(o -> o.getKey().ordinal()))
            .forEach(entry -> results.put(toGatewayKey(entry), toQueryValue(entry)));
        return results;
    }

    /**
     * Get value from Query Parameter Map with key.
     *
     * @param key to look up.
     * @return String content raw
     */
    public String getValue(K key) {
        return luceneParameters.containsKey(key)
            ? luceneParameters.get(key)
            : queryParameters.get(key);
    }

    public String removeValue(K key) {
        return luceneParameters.containsKey(key)
            ? luceneParameters.remove(key)
            : queryParameters.remove(key);
    }

    /**
     * Add a key value pair to Query Parameter Map.
     *
     * @param key   to add to.
     * @param value to assign
     */
    public void setLucineValue(K key, String value) {
        if (nonNull(value)) {
            luceneParameters.put(key, key.encoding() != KeyEncoding.NONE ? decodeUTF(value) : value);
        }
    }

    /**
     * Add a key value pair to Query Parameter Map.
     *
     * @param key   to add to.
     * @param value to assign
     */
    public void setQueryValue(K key, String value) {
        if (nonNull(value)) {
            queryParameters.put(key, key.encoding() != KeyEncoding.NONE ? decodeUTF(value) : value);
        }
    }

    public abstract R doSearch(OpenSearchSwsClient queryClient) throws ApiGatewayException;

    public static Collection<Entry<String, String>> queryToMap(URI uri) {
        return queryToMap(uri.getQuery());
    }

    public static Collection<Entry<String, String>> queryToMap(String query) {
        return
            nonNull(query)
                ? Arrays.stream(query.split(AMPERSAND))
                .map(s -> s.split(EQUAL))
                .map(OpenSearchQuery::stringsToEntry)
                .toList()
                : Collections.emptyList();
    }

    @NotNull
    private static Entry<String, String> stringsToEntry(String... strings) {
        return new Entry<>() {
            @Override
            public String getKey() {
                return strings[0];
            }

            @Override
            public String getValue() {
                return valueOrEmpty(strings);
            }

            @Override
            public String setValue(String value) {
                return null;
            }
        };
    }

    protected String toQueryName(Entry<K, String> entry) {
        return entry.getKey().swsKey().stream().findFirst().orElseThrow();
    }

    protected String toGatewayKey(Entry<K, String> entry) {
        return entry.getKey().key();
    }

    protected String toQueryValue(Entry<K, String> entry) {
        return entry.getKey().encoding() == KeyEncoding.ENCODE_DECODE
            ? encodeUTF(entry.getValue())
            : entry.getValue();
    }

    protected static String mergeParameters(String oldValue, String newValue) {
        if (nonNull(oldValue)) {
            var delimiter = newValue.matches("asc|desc") ? ":" : ",";
            return String.join(delimiter, oldValue, newValue);
        } else {
            return newValue;
        }
    }


    protected static String decodeUTF(String encoded) {
        return URLDecoder.decode(encoded, StandardCharsets.UTF_8);
    }

    protected String encodeUTF(String unencoded) {
        return URLEncoder.encode(unencoded, StandardCharsets.UTF_8).replace(SPACE_ENCODED, PLUS);
    }

    private String toLuceneEntryToString(Entry<K, String> entry) {
        return
            entry.getKey().swsKey().stream()
                .map(swsKey -> entry.getKey().operator().format().formatted(swsKey, toQueryValue(entry)))
                .collect(Collectors.joining(OR, PREFIX, SUFFIX));
    }

    private static String valueOrEmpty(String... strings) {
        return attempt(() -> strings[1]).orElse((f) -> EMPTY_STRING);
    }
}
