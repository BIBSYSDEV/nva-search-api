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
        return queryTools.queryToEntry(key, switch (key.searchOperator()) {
            case GREATER_THAN_OR_EQUAL_TO -> QueryBuilders
                .rangeQuery(searchField)
                .gte(values[0])
                .queryName("GreaterOrEqual-" + key.fieldName());
            case LESS_THAN -> QueryBuilders
                .rangeQuery(searchField)
                .lt(values[0])
                .queryName("LessThan-" + key.fieldName());
            case BETWEEN -> QueryBuilders
                .rangeQuery(searchField)
                .from(values[0])
                .to(values[1])
                .queryName("Between-" + key.fieldName());
            default -> throw new IllegalArgumentException(OPERATOR_NOT_SUPPORTED);
        });
    }
}
