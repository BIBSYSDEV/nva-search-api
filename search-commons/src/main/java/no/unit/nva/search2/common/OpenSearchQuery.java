package no.unit.nva.search2.common;

import static java.util.Objects.nonNull;
import static no.unit.nva.search2.constant.ApplicationConstants.AND;
import static no.unit.nva.search2.constant.ApplicationConstants.OR;
import static no.unit.nva.search2.constant.ApplicationConstants.SEARCH;
import static no.unit.nva.search2.constant.Defaults.HTTPS_SCHEME;
import static nva.commons.core.StringUtils.EMPTY_STRING;
import static nva.commons.core.attempt.Try.attempt;
import java.net.URI;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
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
import no.unit.nva.search2.SwsOpenSearchClient;
import no.unit.nva.search2.model.IParameterKey;
import no.unit.nva.search2.model.IParameterKey.KeyEncoding;
import nva.commons.apigateway.exceptions.ApiGatewayException;
import nva.commons.core.Environment;
import nva.commons.core.paths.UriWrapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@SuppressWarnings({"PMD.Unused", "PMD.LooseCoupling", "PMD.LineLength"})
public abstract class OpenSearchQuery<T extends Enum<T> & IParameterKey, U> {

    // https://www.elastic.co/guide/en/elasticsearch/reference/current/
    // query-dsl-query-string-query.html#query-string-syntax
    protected static final Logger logger = LoggerFactory.getLogger(OpenSearchQuery.class);

    protected static final String API_HOST = new Environment().readEnv("API_HOST");
    protected final transient Map<T, String> queryParameters;
    protected final transient Map<T, String> luceneParameters;
    protected final transient Set<T> otherRequiredKeys;
    protected transient URI gatewayUri;
    protected static final String PREFIX = "(";
    protected static final String SUFFIX = ")";
    protected static final String PLUS = "+";
    protected static final String ENCODED_SPACE = "%20";

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
            new UriWrapper(HTTPS_SCHEME, API_HOST)
                .addChild(SEARCH)
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
                        .map(this::toLuceneParameter)
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
    public String getValue(T key) {
        return luceneParameters.containsKey(key)
                   ? luceneParameters.get(key)
                   : queryParameters.get(key);
    }

    /**
     * Add a key value pair to Query Parameter Map.
     *
     * @param key   to add to.
     * @param value to assign
     */
    public void setValue(T key, String value) {
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
    public void setQValue(T key, String value) {
        if (nonNull(value)) {
            queryParameters.put(key, key.encoding() != KeyEncoding.NONE ? decodeUTF(value) : value);
        }
    }

    public abstract U doSearch(SwsOpenSearchClient queryClient) throws ApiGatewayException;

    public static Map<String, String> queryToMap(URI uri) {
        return queryToMap(uri.getQuery());
    }

    public static Map<String, String> queryToMap(String query) {
        return
            nonNull(query)
                ? Arrays.stream(query.split("&"))
                      .map(s -> s.split("="))
                      .collect(Collectors.toMap(strings -> strings[0], OpenSearchQuery::valueOrEmpty))
                : Collections.emptyMap();
    }

    protected String toQueryName(Entry<T, String> entry) {
        return entry.getKey().swsKey().stream().findFirst().orElseThrow();
    }

    protected String toGatewayKey(Entry<T, String> entry) {
        return entry.getKey().key();
    }

    protected String toQueryValue(Entry<T, String> entry) {
        return entry.getKey().encoding() == KeyEncoding.ENCODE_DECODE
                   ? encodeUTF(entry.getValue())
                   : entry.getValue();
    }

    protected String decodeUTF(String encoded) {
        return URLDecoder.decode(encoded, StandardCharsets.UTF_8);
    }

    protected String encodeUTF(String unencoded) {
        return URLEncoder.encode(unencoded, StandardCharsets.UTF_8).replace(ENCODED_SPACE, PLUS);
    }

    private static String valueOrEmpty(String... strings) {
        return attempt(() -> strings[1]).orElse((f) -> EMPTY_STRING);
    }

    private String toLuceneParameter(Entry<T, String> entry) {
        return
            entry.getKey().swsKey().stream()
                .map(swsKey -> entry.getKey().operator().format().formatted(swsKey, toQueryValue(entry)))
                .collect(Collectors.joining(OR, PREFIX, SUFFIX));
    }
}
