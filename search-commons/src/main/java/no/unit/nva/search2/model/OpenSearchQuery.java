package no.unit.nva.search2.model;

import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;
import static no.unit.nva.search2.constant.ApplicationConstants.AMPERSAND;
import static no.unit.nva.search2.constant.ApplicationConstants.COLON;
import static no.unit.nva.search2.constant.ApplicationConstants.COMMA;
import static no.unit.nva.search2.constant.ApplicationConstants.EQUAL;
import static no.unit.nva.search2.constant.ApplicationConstants.PLUS;
import static no.unit.nva.search2.constant.ApplicationConstants.RESOURCES;
import static no.unit.nva.search2.constant.ApplicationConstants.SEARCH;
import static no.unit.nva.search2.constant.ApplicationConstants.readSearchInfrastructureApiUri;
import static nva.commons.core.StringUtils.EMPTY_STRING;
import static nva.commons.core.StringUtils.SPACE;
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
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Stream;
import no.unit.nva.search2.model.ParameterKey.ValueEncoding;
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
    private transient URI gatewayUri = URI.create("https://unset/resource/search");
    private transient URI openSearchUri = URI.create("https://unset.opensearc");

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
            fromUri(openSearchUri)
                .addChild(RESOURCES, SEARCH)
                .getUri();
    }

    public Map<K, String> getOpenSearchParameters() {
        return luceneParameters;
    }


    /**
     * Query Parameters with string Keys.
     *
     * @return Map of String and String
     */
    public Map<String, String> toNvaSearchApiRequestParameter() {
        var results = new LinkedHashMap<String, String>();
        Stream.of(luceneParameters.entrySet(), queryParameters.entrySet())
            .flatMap(Set::stream)
            .sorted(Comparator.comparingInt(o -> o.getKey().ordinal()))
            .forEach(entry -> results.put(toNvaSearchApiKey(entry), entry.getValue().replace(SPACE, PLUS)));
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

    public Optional<String> getOptional(K key) {
        return Optional.ofNullable(luceneParameters.containsKey(key)
                   ? luceneParameters.get(key)
                   : queryParameters.get(key));
    }

    public String removeKey(K key) {
        return luceneParameters.containsKey(key)
                   ? luceneParameters.remove(key)
                   : queryParameters.remove(key);
    }

    public boolean isPresent(K key) {
        return luceneParameters.containsKey(key) || queryParameters.containsKey(key);
    }


    @JacocoGenerated
    public boolean hasNoSearchValue() {
        return luceneParameters.isEmpty();
    }

    /**
     * Add a key value pair to searchable Parameters.
     *
     * @param key   to add to.
     * @param value to assign
     */
    public void setSearchFieldValue(K key, String value) {
        if (nonNull(value)) {
            var decodedValue = key.valueEncoding() != ValueEncoding.NONE ? decodeUTF(value) : value;
            luceneParameters.put(key, decodedValue);
        }
    }

    /**
     * Add a key value pair to non-searchable Parameters.
     *
     * @param key   to add to.
     * @param value to assign
     */
    public void setQueryValue(K key, String value) {
        if (nonNull(value)) {
            queryParameters.put(key, key.valueEncoding() != ValueEncoding.NONE ? decodeUTF(value) : value);
        }
    }

    public MediaType getMediaType() {
        return mediaType;
    }

    public void setMediaType(String mediaType) {
        if (nonNull(mediaType) && mediaType.contains("text/csv")) {
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

    @NotNull
    private static Entry<String, String> stringsToEntry(String... strings) {
        return new Entry<>() {
            @Override
            public String getKey() {
                return strings[0];
            }

            @Override
            public String getValue() {
                return attempt(() -> strings[1]).orElse((f) -> EMPTY_STRING);
            }

            @Override
            @JacocoGenerated
            public String setValue(String value) {
                return null;
            }
        };
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
            return (T) switch (key.fieldType()) {
                case DATE -> castDateTime();
                case NUMBER -> castNumber();
                default -> value;
            };
        }

        @Override
        public String toString() {
            return value;
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
