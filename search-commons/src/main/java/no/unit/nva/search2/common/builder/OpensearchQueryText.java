package no.unit.nva.search2.common.builder;

import java.util.Arrays;
import java.util.Map;
import java.util.stream.Stream;
import no.unit.nva.search2.enums.ParameterKey;
import no.unit.nva.search2.enums.ParameterKey.FieldOperator;
import org.opensearch.common.unit.Fuzziness;
import org.opensearch.index.query.DisMaxQueryBuilder;
import org.opensearch.index.query.MatchQueryBuilder;
import org.opensearch.index.query.MultiMatchQueryBuilder;
import org.opensearch.index.query.Operator;
import org.opensearch.index.query.QueryBuilder;
import org.opensearch.index.query.QueryBuilders;

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

        if (FieldOperator.SHOULD.equals(key.searchOperator())) {
            return queryToEntry(key, getDisMaxQueryBuilder(values, searchFields));
        } else {
            return (
                hasMoreThanOne(searchFields)
                    ? getMultiMatchQueryBuilderStream(values, searchFields)
                    : getMatchQueryBuilderStream(key, values)
            ).flatMap(builder -> queryToEntry(key, builder));
        }
    }

    private static DisMaxQueryBuilder getDisMaxQueryBuilder(String[] values, String... searchFields) {
        var disMax = QueryBuilders.disMaxQuery();
        Arrays.stream(values).forEach(
            value -> Arrays.stream(searchFields).forEach(
                field -> disMax.add(QueryBuilders.matchQuery(field, value))));
        return disMax;
    }

    private Stream<MatchQueryBuilder> getMatchQueryBuilderStream(K key, String... values) {
        return Arrays.stream(values)
            .map(singleValue -> getMatchQueryBuilder(key, singleValue));
    }

    private Stream<MultiMatchQueryBuilder> getMultiMatchQueryBuilderStream(String[] values, String... searchFields) {
        return Arrays.stream(values)
            .map(singleValue -> getMultiMatchQueryBuilder(singleValue, searchFields));
    }

    public MatchQueryBuilder getMatchQueryBuilder(K key, String singleValue) {
        final var searchField = getFirstSearchField(key);
        return QueryBuilders
            .matchQuery(searchField, singleValue)
            .boost(key.fieldBoost())
            .fuzziness(Fuzziness.AUTO)
            .operator(Operator.AND);
    }

    private MultiMatchQueryBuilder getMultiMatchQueryBuilder(String singleValue, String... searchFields) {
        return QueryBuilders
            .multiMatchQuery(singleValue, searchFields)
            .type(MultiMatchQueryBuilder.Type.BEST_FIELDS)
            .fuzziness(Fuzziness.AUTO)
            .operator(Operator.AND);
    }

}
