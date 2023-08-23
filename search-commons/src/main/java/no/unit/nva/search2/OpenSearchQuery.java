package no.unit.nva.search2;


import no.unit.nva.search.models.SearchResponseDto;
import nva.commons.core.paths.UriWrapper;
import org.opensearch.action.search.SearchRequest;
import org.opensearch.gateway.GatewayException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static java.util.Objects.nonNull;
import static nva.commons.apigateway.RestRequestHandler.EMPTY_STRING;

@SuppressWarnings({"Unused", "LooseCoupling"})
public abstract class OpenSearchQuery<T extends Enum<T> & IParameterKey> {

    protected static final Logger logger = LoggerFactory.getLogger(OpenSearchQuery.class);
    private static final URI API_URL = URI.create("");
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
            UriWrapper.fromUri(API_URL)
                .addChild(getPath())
                .addQueryParameters(toParameters())
                .getUri();
    }


    /**
     * Cristin Query Parameters with string Keys.
     *
     * @return Map of String and String
     */
    public Map<String, String> toParameters() {
        var results =
            Stream.of(queryParameters.entrySet(), pathParameters.entrySet())
                .flatMap(Collection::stream)
                .filter(this::ignorePathParameters)
                .collect(Collectors.toMap(this::toQueryName, this::toQueryValue));
        return new TreeMap<>(results);
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

    /**
     * Builds URI to search Cristin projects based on parameters supplied to the builder methods.
     *
     * @param key   to add to.
     * @param value to assign
     */
    public void setPath(T key, String value) {
        var nonNullValue = nonNull(value) ? value : EMPTY_STRING;
        pathParameters.put(key, key.encoding() == IParameterKey.KeyEncoding.DECODE ? decodeUTF(nonNullValue) : nonNullValue);
    }

    /**
     * Compares content of queryParameters in CristinQuery.
     *
     * @param other CristinQuery to compare
     * @return true if content of Maps are equal
     */
    public boolean areEqual(OpenSearchQuery<T> other) {
        if (queryParameters.size() != other.queryParameters.size()
            || pathParameters.size() != other.pathParameters.size()) {
            return false;
        }

        return
            queryParameters.entrySet().stream()
                .allMatch(e -> e.getValue().equals(other.getValue(e.getKey())))
                &&
                pathParameters.entrySet().stream()
                    .allMatch(e -> e.getValue().equals(other.getValue(e.getKey())));
    }

    /**
     * Query Parameter map contain key.
     *
     * @param key to check
     * @return true if map contains key
     */
    public boolean containsKey(T key) {
        return queryParameters.containsKey(key) || pathParameters.containsKey(key);
    }

    /**
     * Query Parameter map remove value having key.
     *
     * @param key to check
     */
    public void removeValue(T key) {
        if (queryParameters.containsKey(key)) {
            queryParameters.remove(key);
        } else {
            pathParameters.remove(key);
        }
    }

    protected Comparator<Entry<T, String>> byOrdinalDesc() {
        return Comparator.comparingInt(k -> k.getKey().ordinal());
    }

    protected String toQueryName(Entry<T, String> entry) {
        return entry.getKey().getKey();
    }

    protected String toQueryValue(Entry<T, String> entry) {
        return entry.getKey().encoding() == IParameterKey.KeyEncoding.ENCODE_DECODE
            ? encodeUTF(entry.getValue())
            : entry.getValue();
    }

    public abstract SearchResponseDto execute(SwsQueryClient queryClient) throws GatewayException;

    protected SearchRequest toSearchRequest() {
        /// TODO implement builder

        return new SearchRequest();
    }


    /**
     * Sample code for ignorePathParameters.
     * <p>Usage:</p>
     * <samp>return f.getKey() != PATH_PROJECT;<br>
     * </samp>
     */
    protected abstract boolean ignorePathParameters(Entry<T, String> f);


    /**
     * Sample code for getCristinPath.
     * <p>Usage:</p>
     * <samp>var children = containsKey(PATH_PROJECT)<br>
     *     ? new String[]{PATH_PROJECT.getKey(), getValue(PATH_PROJECT)}<br>
     *     : new String[]{PATH_PROJECT.getKey()};<br>
     * return children;<br>
     * </samp>
     */
    protected abstract String[] getPath();



    protected String decodeUTF(String encoded) {
        String decode = URLDecoder.decode(encoded, StandardCharsets.UTF_8);
        logger.info("decoded " + decode);
        return decode;
    }

    protected String encodeUTF(String unencoded) {
        return URLEncoder.encode(unencoded, StandardCharsets.UTF_8).replace("%20", "+");
    }

}
