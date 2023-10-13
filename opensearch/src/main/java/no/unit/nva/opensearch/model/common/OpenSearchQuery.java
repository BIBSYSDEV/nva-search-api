package no.unit.nva.opensearch.model.common;

import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;
import static no.unit.nva.opensearch.constant.Application.AMPERSAND;
import static no.unit.nva.opensearch.constant.Application.AND;
import static no.unit.nva.opensearch.constant.Application.COLON;
import static no.unit.nva.opensearch.constant.Application.COMMA;
import static no.unit.nva.opensearch.constant.Application.EQUAL;
import static no.unit.nva.opensearch.constant.Application.OR;
import static no.unit.nva.opensearch.constant.Application.PREFIX;
import static no.unit.nva.opensearch.constant.Application.RESOURCES;
import static no.unit.nva.opensearch.constant.Application.SEARCH;
import static no.unit.nva.opensearch.constant.Application.SUFFIX;
import static no.unit.nva.opensearch.constant.Application.readSearchInfrastructureApiUri;
import static nva.commons.core.StringUtils.EMPTY_STRING;
import static nva.commons.core.attempt.Try.attempt;
import static nva.commons.core.paths.UriWrapper.fromUri;
import com.google.common.net.MediaType;
import java.net.URI;
import java.net.URLDecoder;
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
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import no.unit.nva.opensearch.model.ResourceSortKeys;
import no.unit.nva.opensearch.model.common.ParameterKey.KeyEncoding;
import no.unit.nva.opensearch.model.common.ParameterKey.ParamKind;
import nva.commons.core.JacocoGenerated;
import org.jetbrains.annotations.NotNull;
import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class OpenSearchQuery<K extends Enum<K> & ParameterKey> {

    protected static final Logger logger = LoggerFactory.getLogger(OpenSearchQuery.class);
    protected final transient Map<K, String> queryParameters;
    protected final transient Map<K, String> luceneParameters;
    protected final transient Set<K> otherRequiredKeys;
    private transient MediaType mediaType;
    private transient URI gatewayUri = URI.create("https://localhost/resource/search");

    protected OpenSearchQuery() {
        luceneParameters = new ConcurrentHashMap<>();
        queryParameters = new ConcurrentHashMap<>();
        otherRequiredKeys = new HashSet<>();
        mediaType = MediaType.JSON_UTF_8;
    }

    /**
     * Builds URI to query SWS based on post body.
     *
     * @return an URI to Sws search without parameters.
     */
    public URI openSearchUri() {
        return
            fromUri(readSearchInfrastructureApiUri())
                .addChild(RESOURCES, SEARCH)
                .getUri();
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
    public AsType getValue(K key) {
        return new AsType(
            luceneParameters.containsKey(key)
                ? luceneParameters.get(key)
                : queryParameters.get(key),
            key
        );
    }

    public String removeValue(K key) {
        return luceneParameters.containsKey(key)
                   ? luceneParameters.remove(key)
                   : queryParameters.remove(key);
    }

    @JacocoGenerated
    public boolean isPresent(K key) {
        return luceneParameters.containsKey(key) || queryParameters.containsKey(key);
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

    public static Collection<Entry<String, String>> queryToMapEntries(URI uri) {
        return queryToMapEntries(uri.getQuery());
    }

    public static Collection<Entry<String, String>> queryToMapEntries(String query) {
        return
            nonNull(query)
                ? Arrays.stream(query.split(AMPERSAND))
                      .map(s -> s.split(EQUAL))
                      .map(OpenSearchQuery::stringsToEntry)
                      .toList()
                : Collections.emptyList();
    }

    public MediaType getMediaType() {
        return mediaType;
    }

    public void setMediaType(String mediaType) {
        if (nonNull(mediaType) && mediaType.contains("text/")) {
            this.mediaType = MediaType.CSV_UTF_8;
        } else {
            this.mediaType = MediaType.JSON_UTF_8;
        }
    }

    public URI getGatewayUri() {
        return gatewayUri;
    }

    public void setGatewayUri(URI gatewayUri) {
        this.gatewayUri = gatewayUri;
    }


    protected String toGatewayKey(Entry<K, String> entry) {
        return entry.getKey().key();
    }

    protected String toQueryValue(Entry<K, String> entry) {
        return entry.getKey().kind() == ParamKind.SORT_STRING
                   ? Arrays.stream(entry.getValue().split(COMMA))
                         .map(sort -> sort.split(COLON))
                         .map(this::expandSortKeys)
                         .collect(Collectors.joining(COMMA))
                   : entry.getValue();
    }

    protected static String mergeParameters(String oldValue, String newValue) {
        if (nonNull(oldValue)) {
            var delimiter = newValue.matches("asc|desc") ? COLON : COMMA;
            return String.join(delimiter, oldValue, newValue);
        } else {
            return newValue;
        }
    }

    protected static String decodeUTF(String encoded) {
        return URLDecoder.decode(encoded, StandardCharsets.UTF_8);
    }

//    protected String encodeUTF(String unencoded) {
//        return URLEncoder.encode(unencoded, StandardCharsets.UTF_8).replace(SPACE_ENCODED, PLUS);
//    }

    private String toLuceneEntryToString(Entry<K, String> entry) {
        return
            entry.getKey().swsKey().stream()
                .map(swsKey -> entry.getKey().operator().format().formatted(swsKey, toQueryValue(entry)))
                .collect(Collectors.joining(OR, PREFIX, SUFFIX));
    }

    private String expandSortKeys(String... strings) {
        var sortOrder = strings[1];
        var luceneKey = ResourceSortKeys.keyFromString(strings[0]).getFieldName();
        return luceneKey + COLON + sortOrder;
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
            @JacocoGenerated
            public String setValue(String value) {
                return null;
            }
        };
    }

    private static String valueOrEmpty(String... strings) {
        return attempt(() -> strings[1]).orElse((f) -> EMPTY_STRING);
    }

    @SuppressWarnings({"PMD.ShortMethodName"})
    public static class AsType {

        private final String value;
        private final ParameterKey key;

        public AsType(String value, ParameterKey key) {
            this.value = value;
            this.key = key;
        }

        public <T> T as() {
            if (isNull(value)) {
                return null;
            }
            return (T) switch (key.kind()) {
                case DATE -> castDateTime();
                case NUMBER -> castNumber();
                default -> value;
            };
        }

        private <T> T castDateTime() {
            return ((Class<T>) DateTime.class).cast(DateTime.parse(value));
        }



        @NotNull
        private <T extends Number> T castNumber() {
            return (T) attempt(() -> Integer.parseInt(value)).orElseThrow();
        }
    }
}
