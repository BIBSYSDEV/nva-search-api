package no.unit.nva.search2.common.builder;

import static no.unit.nva.search2.common.constant.Words.KEYWORD_FALSE;
import java.util.Arrays;
import java.util.Map.Entry;
import java.util.stream.Stream;
import no.unit.nva.search2.common.enums.ParameterKey;
import org.opensearch.index.query.DisMaxQueryBuilder;
import org.opensearch.index.query.Operator;
import org.opensearch.index.query.QueryBuilder;
import org.opensearch.index.query.QueryBuilders;

import static org.opensearch.index.query.QueryBuilders.matchPhrasePrefixQuery;
import static org.opensearch.index.query.QueryBuilders.matchQuery;

public class QueryBuilderText<K extends Enum<K> & ParameterKey> extends AbstractQueryBuilder<K> {

    @Override
    protected Stream<Entry<K, QueryBuilder>> buildMatchAnyKeyValuesQuery(K key, String... values) {
        return buildAnyComboMustHitQuery(key, values)
            .flatMap(builder -> queryTools.queryToEntry(key, builder));
    }

    @Override
    protected Stream<Entry<K, QueryBuilder>> buildMatchAllValuesQuery(K key, String... values) {
        return buildAllMustHitQuery(key, values)
            .flatMap(builder -> queryTools.queryToEntry(key, builder));
    }

    private Stream<QueryBuilder> buildAllMustHitQuery(K key, String... values) {
        return Arrays.stream(values)
            .map(singleValue -> phrasePrefixBuilder(singleValue, key)
                .collect(DisMaxQueryBuilder::new, DisMaxQueryBuilder::add, DisMaxQueryBuilder::add)
                .queryName("TextAll-" + key.asCamelCase()));
    }

    private Stream<DisMaxQueryBuilder> buildAnyComboMustHitQuery(K key, String... values) {
        var disMax = QueryBuilders.disMaxQuery().queryName("TextAny-" + key.asCamelCase());
        Arrays.stream(values)
            .flatMap(singleValue -> phrasePrefixBuilder(singleValue, key))
            .forEach(disMax::add);
        return Stream.of(disMax);
    }

    private Stream<QueryBuilder> phrasePrefixBuilder(String singleValue, K key) {
        return Stream.concat(
            key.searchFields(KEYWORD_FALSE).map(fieldName -> matchPhrasePrefixQuery(fieldName, singleValue)
                .boost(key.fieldBoost() + 0.1F)),
            key.searchFields(KEYWORD_FALSE).map(fieldName -> matchQuery(fieldName, singleValue)
                .operator(Operator.AND)
                .boost(key.fieldBoost())));
    }
}