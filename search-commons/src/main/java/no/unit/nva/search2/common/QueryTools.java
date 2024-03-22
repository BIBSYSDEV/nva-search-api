package no.unit.nva.search2.common;

import static java.util.Objects.nonNull;
import static no.unit.nva.search2.common.constant.Defaults.DEFAULT_SORT_ORDER;
import static no.unit.nva.search2.common.constant.Patterns.COLON_OR_SPACE;
import static nva.commons.core.attempt.Try.attempt;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.Collection;
import java.util.Map;
import java.util.Map.Entry;
import java.util.stream.Stream;
import no.unit.nva.search2.common.enums.ParameterKey;
import nva.commons.core.JacocoGenerated;
import org.opensearch.index.query.QueryBuilder;
import org.opensearch.search.sort.SortOrder;

public final class QueryTools<K extends Enum<K> & ParameterKey> {

    public static boolean hasContent(String value) {
        return nonNull(value) && !value.isEmpty();
    }

    public static boolean hasContent(Collection<?> value) {
        return nonNull(value) && !value.isEmpty();
    }

    public static String decodeUTF(String encoded) {
        return URLDecoder.decode(encoded, StandardCharsets.UTF_8);
    }

    public static Entry<String, SortOrder> objectToSortEntry(String sortString) {
        return stringsToSortEntry(sortString.split(COLON_OR_SPACE));
    }

    public static Entry<String, SortOrder> stringsToSortEntry(String... strings) {
        return new Entry<>() {
            @Override
            public String getKey() {
                return strings[0];
            }

            @Override
            public SortOrder getValue() {
                final var orderString = attempt(() -> strings[1])
                    .orElse((f) -> DEFAULT_SORT_ORDER);
                return SortOrder.fromString(orderString);
            }

            @Override
            @JacocoGenerated
            public SortOrder setValue(SortOrder value) {
                return null;
            }
        };
    }

    public String getFirstSearchField(K key) {
        return key.searchFields().findFirst().orElseThrow();
    }

    public Stream<Map.Entry<K, QueryBuilder>> queryToEntry(K key, QueryBuilder qb) {
        final var entry = new Map.Entry<K, QueryBuilder>() {
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
}
