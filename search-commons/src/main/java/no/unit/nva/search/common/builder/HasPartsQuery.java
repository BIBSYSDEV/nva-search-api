package no.unit.nva.search.common.builder;

import static no.unit.nva.search.common.constant.Words.PART_OF;

import no.unit.nva.search.common.constant.Functions;
import no.unit.nva.search.common.enums.ParameterKey;

import nva.commons.core.JacocoGenerated;

import org.apache.lucene.search.join.ScoreMode;
import org.opensearch.index.query.QueryBuilder;
import org.opensearch.join.query.HasChildQueryBuilder;

import java.util.Map;
import java.util.stream.Stream;

public class HasPartsQuery<K extends Enum<K> & ParameterKey<K>> extends AbstractBuilder<K> {

    @Override
    @JacocoGenerated // not currently in use...
    protected Stream<Map.Entry<K, QueryBuilder>> buildMatchAnyKeyValuesQuery(
            K key, String... values) {
        return buildHasChildQuery(key, values)
                .flatMap(builder -> Functions.queryToEntry(key, builder));
    }

    @Override
    protected Stream<Map.Entry<K, QueryBuilder>> buildMatchAllValuesQuery(K key, String... values) {
        return buildHasChildQuery(key, values)
                .flatMap(builder -> Functions.queryToEntry(key, builder));
    }

    private Stream<QueryBuilder> buildHasChildQuery(K key, String... values) {
        var builder =
                new HasChildQueryBuilder(
                        PART_OF, getSubQuery(key.subQuery(), values), ScoreMode.None);
        return Stream.of(builder);
    }
}
