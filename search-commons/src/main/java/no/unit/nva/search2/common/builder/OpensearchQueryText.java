package no.unit.nva.search2.common.builder;

import java.util.Arrays;
import java.util.Map.Entry;
import java.util.stream.Stream;
import no.unit.nva.search2.common.enums.ParameterKey;
import org.opensearch.index.query.DisMaxQueryBuilder;
import org.opensearch.index.query.MatchPhrasePrefixQueryBuilder;
import org.opensearch.index.query.QueryBuilder;
import org.opensearch.index.query.QueryBuilders;

public class OpensearchQueryText<K extends Enum<K> & ParameterKey> extends OpensearchQuery<K> {

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
                .queryName("TextAll-" + key.fieldName()));
    }

    private Stream<DisMaxQueryBuilder> buildAnyComboMustHitQuery(K key, String... values) {
        var disMax = QueryBuilders.disMaxQuery().queryName("TextAny-" + key.fieldName());
        Arrays.stream(values)
            .flatMap(singleValue -> phrasePrefixBuilder(singleValue, key))
            .forEach(disMax::add);
        return Stream.of(disMax);
    }

    private Stream<MatchPhrasePrefixQueryBuilder> phrasePrefixBuilder(String singleValue, K key) {
        return key.searchFields()
            .map(fieldName -> QueryBuilders.matchPhrasePrefixQuery(fieldName, singleValue));
    }
}