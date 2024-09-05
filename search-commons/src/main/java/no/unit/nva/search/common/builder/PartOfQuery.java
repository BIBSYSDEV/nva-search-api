package no.unit.nva.search.common.builder;

import static no.unit.nva.search.common.constant.Words.HAS_PARTS;

import no.unit.nva.search.common.constant.Functions;
import no.unit.nva.search.common.enums.ParameterKey;

import nva.commons.core.JacocoGenerated;

import org.opensearch.index.query.QueryBuilder;
import org.opensearch.join.query.HasParentQueryBuilder;

import java.util.Map;
import java.util.stream.Stream;

public class PartOfQuery<K extends Enum<K> & ParameterKey<K>> extends AbstractBuilder<K> {

    @Override
    @JacocoGenerated // not currently in use...
    protected Stream<Map.Entry<K, QueryBuilder>> buildMatchAnyKeyValuesQuery(
            K key, String... values) {
        return buildHitQuery(key, values).flatMap(builder -> Functions.queryToEntry(key, builder));
    }

    @Override
    protected Stream<Map.Entry<K, QueryBuilder>> buildMatchAllValuesQuery(K key, String... values) {
        return buildHitQuery(key, values).flatMap(builder -> Functions.queryToEntry(key, builder));
    }

    private Stream<QueryBuilder> buildHitQuery(K key, String... values) {
        var builder =
                new HasParentQueryBuilder(HAS_PARTS, getSubQuery(key.subQuery(), values), true);
        return Stream.of(builder);
    }
}
