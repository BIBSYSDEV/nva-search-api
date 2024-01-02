package no.unit.nva.search2.common.builder;

import no.unit.nva.search2.enums.ParameterKey;
import org.opensearch.index.query.QueryBuilder;
import org.opensearch.index.query.QueryBuilders;

import java.util.Map;
import java.util.stream.Stream;

import static no.unit.nva.search2.constant.ErrorMessages.OPERATOR_NOT_SUPPORTED;

public class OpensearchQueryRange<K extends Enum<K> & ParameterKey> extends OpensearchQuery<K> {
    @Override
    protected Stream<Map.Entry<K, QueryBuilder>> valueQuery(K key, String value) {
        final var searchField = getFirstSearchField(key);

        return queryToEntry(key, switch (key.searchOperator()) {
            case MUST, MUST_NOT, SHOULD -> throw new IllegalArgumentException(OPERATOR_NOT_SUPPORTED);
            case GREATER_THAN_OR_EQUAL_TO -> QueryBuilders.rangeQuery(searchField).gte(value);
            case LESS_THAN -> QueryBuilders.rangeQuery(searchField).lt(value);
        });
    }

    @Override
    protected Stream<Map.Entry<K, QueryBuilder>> multiValueQuery(K key, String... values) {
        final var searchField = getFirstSearchField(key);

        return queryToEntry(key, switch (key.searchOperator()) {
            case MUST, MUST_NOT, SHOULD -> throw new IllegalArgumentException(OPERATOR_NOT_SUPPORTED);
            case LESS_THAN, GREATER_THAN_OR_EQUAL_TO ->
                QueryBuilders.rangeQuery(searchField).from(values[0]).to(values[1]);
        });
    }
}
