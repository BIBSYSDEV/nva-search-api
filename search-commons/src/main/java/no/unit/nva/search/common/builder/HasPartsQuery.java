package no.unit.nva.search.common.builder;

import java.util.Map;
import java.util.stream.Stream;
import no.unit.nva.search.common.constant.Functions;
import no.unit.nva.search.common.enums.ParameterKey;
import org.apache.lucene.search.join.ScoreMode;
import org.opensearch.index.query.QueryBuilder;
import org.opensearch.join.query.HasChildQueryBuilder;

public class HasPartsQuery<K extends Enum<K> & ParameterKey<K>> extends AbstractBuilder<K> {

    @Override
    protected Stream<Map.Entry<K, QueryBuilder>> buildMatchAnyKeyValuesQuery(K key, String... values) {
        return buildAnyComboMustHitQuery(key, values)
            .flatMap(builder -> Functions.queryToEntry(key, builder));
    }

    @Override
    protected Stream<Map.Entry<K, QueryBuilder>> buildMatchAllValuesQuery(K key, String... values) {
        return buildAllMustHitQuery(key, values)
            .flatMap(builder -> Functions.queryToEntry(key, builder));
    }

    private Stream<QueryBuilder> buildAllMustHitQuery(K key, String... values) {
        var builder =
            new HasChildQueryBuilder("partOf", getSubQuery(key.subQuery(), values), ScoreMode.None);
        return Stream.of(builder);
    }

    private Stream<QueryBuilder> buildAnyComboMustHitQuery(K key, String... values) {
        var builder =
            new HasChildQueryBuilder("partOf", getSubQuery(key.subQuery(), values), ScoreMode.None);
        return Stream.of(builder);
    }
}
