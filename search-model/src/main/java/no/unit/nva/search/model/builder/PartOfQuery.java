package no.unit.nva.search.model.builder;

import static no.unit.nva.search.model.constant.Functions.queryToEntry;
import static no.unit.nva.search.model.constant.Words.HAS_PARTS;

import no.unit.nva.search.model.enums.ParameterKey;

import nva.commons.core.JacocoGenerated;
import org.opensearch.index.query.QueryBuilder;
import org.opensearch.join.query.HasParentQueryBuilder;

import java.util.Map;
import java.util.stream.Stream;

/**
 * Class for building OpenSearch queries that search for parts of a document.
 *
 * @author Stig Norland
 * @param <K> the type of the parameter key.
 */
public class PartOfQuery<K extends Enum<K> & ParameterKey<K>> extends AbstractBuilder<K> {

    @JacocoGenerated
    @Override
    Stream<Map.Entry<K, QueryBuilder>> buildMatchAnyValueQuery(K key, String... values) {
        return buildHasParent(key, values).flatMap(builder -> queryToEntry(key, builder));
    }

    @Override
    Stream<Map.Entry<K, QueryBuilder>> buildMatchAllValuesQuery(K key, String... values) {
        return buildHasParent(key, values).flatMap(builder -> queryToEntry(key, builder));
    }

    private Stream<QueryBuilder> buildHasParent(K key, String... values) {
        var builder =
                new HasParentQueryBuilder(HAS_PARTS, getSubQuery(key.subQuery(), values), true);
        return Stream.of(builder);
    }
}
