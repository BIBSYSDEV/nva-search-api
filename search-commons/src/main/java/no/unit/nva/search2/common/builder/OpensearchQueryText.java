package no.unit.nva.search2.common.builder;

import no.unit.nva.search2.common.ParameterKey;
import org.opensearch.common.unit.Fuzziness;
import org.opensearch.index.query.MatchQueryBuilder;
import org.opensearch.index.query.MultiMatchQueryBuilder;
import org.opensearch.index.query.Operator;
import org.opensearch.index.query.QueryBuilder;
import org.opensearch.index.query.QueryBuilders;

import java.util.Arrays;
import java.util.Map;
import java.util.stream.Stream;

public class OpensearchQueryText<K extends Enum<K> & ParameterKey> extends OpensearchQuery<K> {

    @Override
    protected Stream<Map.Entry<K, QueryBuilder>> valueQuery(K key, String value) {
        final var searchFields = getSearchFields(key);
        if (hasMoreThanOne(searchFields)) {
            return queryToEntry(key, getMultiMatchQueryBuilder(value, searchFields));
        }
        return queryToEntry(key, getMatchQueryBuilder(key, value));
    }

    @Override
    protected Stream<Map.Entry<K, QueryBuilder>> multiValueQuery(K key, String... values) {
        final var searchFields = getSearchFields(key);
        if (hasMoreThanOne(searchFields)) {
            return Arrays.stream(values)
                .map(singleValue -> getMultiMatchQueryBuilder(singleValue, searchFields))
                .flatMap(builder -> queryToEntry(key, builder));
        } else {
            return Arrays.stream(values)
                .map(singleValue -> getMatchQueryBuilder(key, singleValue))
                .flatMap(builder -> queryToEntry(key, builder));
        }
    }

     public MatchQueryBuilder getMatchQueryBuilder(K key, String singleValue) {
        final var searchField = getFirstSearchField(key);
        return QueryBuilders
            .matchQuery(searchField, singleValue)
            .boost(key.fieldBoost())
            .fuzziness(Fuzziness.AUTO)
            .operator(operatorByKey(key));
    }

    private MultiMatchQueryBuilder getMultiMatchQueryBuilder(String singleValue, String... searchFields) {
        return QueryBuilders
            .multiMatchQuery(singleValue, searchFields)
            .fuzziness(Fuzziness.AUTO)
            .type(MultiMatchQueryBuilder.Type.BEST_FIELDS)
            .operator(Operator.OR);
    }

}
