package no.unit.nva.search2.common.builder;

import static no.unit.nva.search2.constant.ErrorMessages.OPERATOR_NOT_SUPPORTED;
import java.util.Map.Entry;
import java.util.stream.Stream;
import no.unit.nva.search2.enums.ParameterKey;
import org.opensearch.index.query.QueryBuilder;
import org.opensearch.index.query.QueryBuilders;

public class OpensearchQueryRange<K extends Enum<K> & ParameterKey> extends OpensearchQuery<K> {

    @Override
    protected Stream<Entry<K, QueryBuilder>> queryAsEntryStream(K key, String... values) {
        final var searchField = queryTools.getFirstSearchField(key);
        return queryTools.queryToEntry(key, switch (key.searchOperator()) {
            case MUST, MUST_NOT, SHOULD -> throw new IllegalArgumentException(OPERATOR_NOT_SUPPORTED);
            case GREATER_THAN_OR_EQUAL_TO -> QueryBuilders.rangeQuery(searchField).gte(values[0]);
            case LESS_THAN -> QueryBuilders.rangeQuery(searchField).lt(values[0]);
            case BETWEEN -> QueryBuilders.rangeQuery(searchField).from(values[0]).to(values[1]);
        });
    }

}
