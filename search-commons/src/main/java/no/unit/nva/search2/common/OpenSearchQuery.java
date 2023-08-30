package no.unit.nva.search2.common;

import static java.util.Objects.nonNull;
import static no.unit.nva.search.restclients.IdentityClientImpl.HTTPS_SCHEME;
import java.net.URI;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Collection;
import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import no.unit.nva.search.models.SearchResponseDto;
import no.unit.nva.search2.SwsOpenSearchClient;
import nva.commons.apigateway.exceptions.ApiGatewayException;
import nva.commons.core.Environment;
import nva.commons.core.paths.UriWrapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@SuppressWarnings({"Unused", "LooseCoupling"})
public abstract class OpenSearchQuery<T extends Enum<T> & IParameterKey> {
    // https://www.elastic.co/guide/en/elasticsearch/reference/current/query-dsl-query-string-query.html#query-string-syntax
    protected static final Logger logger = LoggerFactory.getLogger(OpenSearchQuery.class);

    protected static final String API_HOST = new Environment().readEnv("API_HOST");
    protected final transient Map<T, String> pathParameters;
    protected final transient Map<T, String> queryParameters;
    protected final transient Set<T> otherRequiredKeys;

    protected OpenSearchQuery() {
        queryParameters = new ConcurrentHashMap<>();
        pathParameters = new ConcurrentHashMap<>();
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
                .addChild()
                .addQueryParameters(toLuceneParameter())
                .getUri();
    }


    /**
     * Query Parameters with string Keys.
     *
     * @return Map of String and String
     */
    public Map<String, String> toParameters() {
        var results =
            Stream.of(queryParameters.entrySet(), pathParameters.entrySet())
                .flatMap(Collection::stream)
                .collect(Collectors.toMap(this::toQueryName, this::toQueryValue));
        return new TreeMap<>(results);
    }

    /**
     * Query Parameters with string Keys.
     *
     * @return Map of String and String
     */
    public Map<String, String> toLuceneParameter() {
        var query = queryParameters.entrySet().stream()
                .map(this::apply)
                .collect(Collectors.joining(" AND "));
        return Map.of("q", query);
    }

    /**
     * Get value from Query Parameter Map with key.
     *
     * @param key to look up.
     * @return String content raw
     */
    public String getValue(T key) {
        return queryParameters.containsKey(key)
            ? queryParameters.get(key)
            : pathParameters.get(key);
    }

    /**
     * Add a key value pair to Query Parameter Map.
     *
     * @param key   to add to.
     * @param value to assign
     */
    public void setValue(T key, String value) {
        if (nonNull(value)) {
            queryParameters.put(key, key.encoding() != IParameterKey.KeyEncoding.NONE ? decodeUTF(value) : value);
        }
    }

    protected String toQueryName(Entry<T, String> entry) {
        return entry.getKey().getKey();
    }

    protected String toQueryValue(Entry<T, String> entry) {
        return entry.getKey().encoding() == IParameterKey.KeyEncoding.ENCODE_DECODE
            ? encodeUTF(entry.getValue())
            : entry.getValue();
    }

    public abstract SearchResponseDto execute(SwsOpenSearchClient queryClient) throws ApiGatewayException;


    protected String decodeUTF(String encoded) {
        String decode = URLDecoder.decode(encoded, StandardCharsets.UTF_8);
        logger.info("decoded " + decode);
        return decode;
    }

    protected String encodeUTF(String unencoded) {
        return URLEncoder.encode(unencoded, StandardCharsets.UTF_8).replace("%20", "+");
    }

    private String apply(Entry<T, String> entry) {
        return entry.getKey().getSwsKey().stream().map(swsKey -> {
            var substring = switch (entry.getKey().getOperator()){
                case EQUALS -> " %s:%s ".formatted(swsKey, entry.getValue());
                case GREATER_THAN, GREATER_THAN_OR_EQUAL_TO -> " %s:>%s ".formatted(swsKey, entry.getValue());
                case LESS_THAN, LESS_THAN_OR_EQUAL_TO -> " %s:<v ".formatted(swsKey, entry.getValue());
            };
            return substring;
//            " {0}:[{1} TO {2}] " .formatted(swsKey, entry.getValue(), entry.getValue());

        }).collect(Collectors.joining(" OR ", "(", ")"));
    }
}
