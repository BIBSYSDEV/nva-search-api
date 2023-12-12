package no.unit.nva.search2.common;

import static java.util.Objects.nonNull;
import static no.unit.nva.search2.constant.Words.AMPERSAND;
import static no.unit.nva.search2.constant.Words.COMMA;
import static no.unit.nva.search2.constant.Words.EQUAL;
import static no.unit.nva.search2.constant.Words.ONE;
import static nva.commons.core.StringUtils.EMPTY_STRING;
import static nva.commons.core.attempt.Try.attempt;
import static nva.commons.core.paths.UriWrapper.fromUri;
import java.net.URI;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.Map.Entry;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import no.unit.nva.search2.constant.Defaults;
import no.unit.nva.search2.constant.Words;
import no.unit.nva.search2.enums.ParameterKey;
import nva.commons.core.JacocoGenerated;
import org.opensearch.index.query.QueryBuilder;
import org.opensearch.search.sort.SortOrder;

public final class QueryTools {

    public static Boolean valueToBoolean(String value) {
        return ONE.equals(value) ? Boolean.TRUE : Boolean.valueOf(value);
    }

    public static URI nextResultsBySortKey(SwsResponse response, Map<String, String> requestParameter, URI gatewayUri) {

        requestParameter.remove(Words.FROM);
        var sortedP =
            response.getSort().stream()
                .map(Object::toString)
                .collect(Collectors.joining(COMMA));
        requestParameter.put(Words.SEARCH_AFTER, sortedP);
        return fromUri(gatewayUri)
            .addQueryParameters(requestParameter)
            .getUri();
    }

    static String[] splitValues(String value) {
        return Arrays.stream(value.split(COMMA))
            .map(String::trim)
            .toArray(String[]::new);
    }

    public static Collection<Entry<String, String>> queryToMapEntries(URI uri) {
        return queryToMapEntries(uri.getQuery());
    }

    public static Collection<Entry<String, String>> queryToMapEntries(String query) {
        return
            nonNull(query)
                ? Arrays.stream(query.split(AMPERSAND))
                .map(s -> s.split(EQUAL))
                .map(QueryTools::stringsToEntry)
                .toList()
                : Collections.emptyList();
    }

    public static <K extends Enum<K> & ParameterKey> Stream<Entry<K, QueryBuilder>> queryToEntry(
        K key, QueryBuilder qb) {
        final var entry = new Entry<K, QueryBuilder>() {
            @Override
            public K getKey() {
                return key;
            }

            @Override
            public QueryBuilder getValue() {
                return qb;
            }

            @Override
            @JacocoGenerated
            public QueryBuilder setValue(QueryBuilder value) {
                return null;
            }
        };
        return Stream.of(entry);
    }

    public static Entry<String, String> stringsToEntry(String... strings) {
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

    public static Entry<String, SortOrder> entryToSortEntry(Entry<String, String> entry) {
        return new Entry<>() {
            @Override
            public String getKey() {
                return entry.getKey();
            }

            @Override
            public SortOrder getValue() {
                var sortOrder = nonNull(entry.getValue()) && !entry.getValue().isEmpty()
                    ? entry.getValue()
                    : Defaults.DEFAULT_SORT_ORDER;
                return SortOrder.fromString(sortOrder);
            }

            @Override
            @JacocoGenerated
            public SortOrder setValue(SortOrder value) {
                return null;
            }
        };
    }
}
