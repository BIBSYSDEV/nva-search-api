package no.unit.nva.search2.common.builder;

import static no.unit.nva.search2.common.constant.ErrorMessages.OPERATOR_NOT_SUPPORTED;
import java.util.Map.Entry;
import java.util.stream.Stream;
import no.unit.nva.search2.common.enums.ParameterKey;
import nva.commons.core.JacocoGenerated;
import org.opensearch.index.query.QueryBuilder;
import org.opensearch.index.query.QueryBuilders;

public class OpensearchQueryRange<K extends Enum<K> & ParameterKey> extends OpensearchQuery<K> {

    @Override
    protected Stream<Entry<K, QueryBuilder>> buildMatchAnyKeyValuesQuery(K key, String... values) {
        return queryAsEntryStream(key, values);
    }

    @JacocoGenerated    // never used
    @Override
    protected Stream<Entry<K, QueryBuilder>> buildMatchAllValuesQuery(K key, String... values) {
        return queryAsEntryStream(key, values);
    }

    protected Stream<Entry<K, QueryBuilder>> queryAsEntryStream(K key, String... values) {
        final var searchField = queryTools.getFirstSearchField(key);
        final var firstParam = valueOrNull(values[0]);
        final var secondParam = values.length == 2 ? valueOrNull(values[1]) : null;
        return queryTools.queryToEntry(key, switch (key.searchOperator()) {
            case GREATER_THAN_OR_EQUAL_TO -> QueryBuilders
                .rangeQuery(searchField)
                .gte(firstParam)
                .queryName("GreaterOrEqual-" + key.fieldName());
            case LESS_THAN -> QueryBuilders
                .rangeQuery(searchField)
                .lt(firstParam)
                .queryName("LessThan-" + key.fieldName());
            case BETWEEN -> QueryBuilders
                .rangeQuery(searchField)
                .from(firstParam)
                .to(secondParam)
                .queryName("Between-" + key.fieldName());
            default -> throw new IllegalArgumentException(OPERATOR_NOT_SUPPORTED);
        });
    }

    private String valueOrNull(String value) {
        return value.isBlank() ? null : value;
    }
}
