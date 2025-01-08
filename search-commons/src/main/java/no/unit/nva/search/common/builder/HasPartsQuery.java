package no.unit.nva.search.common.builder;

import static no.unit.nva.constants.Words.PART_OF;

import java.util.Map;
import java.util.stream.Stream;
import no.unit.nva.search.common.constant.Functions;
import no.unit.nva.search.common.enums.ParameterKey;
import nva.commons.core.JacocoGenerated;
import org.apache.lucene.search.join.ScoreMode;
import org.opensearch.index.query.QueryBuilder;
import org.opensearch.join.query.HasChildQueryBuilder;

/**
 * Class for building OpenSearch queries that search for parts of a document.
 *
 * @param <K> the type of the parameter key.
 * @author Stig Norland
 */
public class HasPartsQuery<K extends Enum<K> & ParameterKey<K>> extends AbstractBuilder<K> {

    @Override
    @JacocoGenerated
    protected Stream<Map.Entry<K, QueryBuilder>> buildMatchAnyValueQuery(K key, String... values) {
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
