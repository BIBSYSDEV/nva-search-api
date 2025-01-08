package no.unit.nva.search.common.builder;

import static no.unit.nva.constants.Words.HAS_PARTS;

import java.util.Map;
import java.util.stream.Stream;
import no.unit.nva.search.common.constant.Functions;
import no.unit.nva.search.common.enums.ParameterKey;
import org.opensearch.index.query.QueryBuilder;
import org.opensearch.join.query.HasParentQueryBuilder;

/**
 * Class for building OpenSearch queries that search for parts of a document.
 *
 * @author Stig Norland
 * @param <K> the type of the parameter key.
 */
public class PartOfQuery<K extends Enum<K> & ParameterKey<K>> extends AbstractBuilder<K> {

    @Override
    Stream<Map.Entry<K, QueryBuilder>> buildMatchAnyValueQuery(K key, String... values) {
        return buildHasParent(key, values).flatMap(builder -> Functions.queryToEntry(key, builder));
    }

    @Override
    Stream<Map.Entry<K, QueryBuilder>> buildMatchAllValuesQuery(K key, String... values) {
        return buildHasParent(key, values).flatMap(builder -> Functions.queryToEntry(key, builder));
    }

    private Stream<QueryBuilder> buildHasParent(K key, String... values) {
        var builder =
                new HasParentQueryBuilder(HAS_PARTS, getSubQuery(key.subQuery(), values), true);
        return Stream.of(builder);
    }
}
