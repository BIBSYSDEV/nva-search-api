package no.unit.nva.search2.common;

import static java.util.Objects.nonNull;
import static no.unit.nva.search2.constants.Defaults.HTTPS_SCHEME;
import static nva.commons.core.attempt.Try.attempt;
import java.net.URI;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

import no.unit.nva.search2.SwsOpenSearchClient;
import nva.commons.apigateway.exceptions.ApiGatewayException;
import nva.commons.core.Environment;
import nva.commons.core.paths.UriWrapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@SuppressWarnings({"PMD.Unused", "PMD.LooseCoupling", "PMD.LineLength"})
public abstract class OpenSearchQuery<T extends Enum<T> & IParameterKey> {
    // https://www.elastic.co/guide/en/elasticsearch/reference/current/query-dsl-query-string-query.html#query-string-syntax
    protected static final Logger logger = LoggerFactory.getLogger(OpenSearchQuery.class);

    protected static final String API_HOST = new Environment().readEnv("API_HOST");
    protected final transient Map<T, String> queryParameters;
    protected final transient Map<T, String> luceneParameters;
    protected final transient Set<T> otherRequiredKeys;

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
    public URI toURI() {
        return
            new UriWrapper(HTTPS_SCHEME, API_HOST)
                .addChild("_search")
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
                .collect(Collectors.joining("+AND+"));
        return Map.of("q", query);
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
            luceneParameters.put(key, key.encoding() != IParameterKey.KeyEncoding.NONE ? decodeUTF(value) : value);
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
            queryParameters.put(key, key.encoding() != IParameterKey.KeyEncoding.NONE ? decodeUTF(value) : value);
        }
    }

    public abstract PagedSearchResponseDto doSearch(SwsOpenSearchClient queryClient) throws ApiGatewayException;


    public static Map<String, String> queryToMap(URI uri) {
        return Arrays
            .stream(uri.getQuery().split("&"))
            .map(s -> s.split("="))
            .collect(Collectors.toMap(strings -> strings[0], OpenSearchQuery::valueOrEmpty));
    }

    protected String toQueryName(Entry<T, String> entry) {
        return entry.getKey().swsKey().stream().findFirst().orElseThrow();
    }

    protected String toQueryValue(Entry<T, String> entry) {
        return entry.getKey().encoding() == IParameterKey.KeyEncoding.ENCODE_DECODE
            ? encodeUTF(entry.getValue())
            : entry.getValue();
    }

    protected String decodeUTF(String encoded) {
        String decode = URLDecoder.decode(encoded, StandardCharsets.UTF_8);
        logger.info("decoded " + decode);
        return decode;
    }

    protected String encodeUTF(String unencoded) {
        return URLEncoder.encode(unencoded, StandardCharsets.UTF_8).replace("%20", "+");
    }

    private static String valueOrEmpty(String... strings) {
        return attempt(() -> strings[1]).orElse((f) -> "");
    }

    private String toLuceneParameter(Entry<T, String> entry) {
        return
            entry.getKey().swsKey().stream()
                .map(swsKey -> entry.getKey().operator().format().formatted(swsKey, toQueryValue(entry)))
                .collect(Collectors.joining("+OR+", "(", ")"));
    }

}
