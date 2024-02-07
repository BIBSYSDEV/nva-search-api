package no.unit.nva.search2.common.builder;

import java.util.Arrays;
import java.util.Map;
import java.util.stream.Stream;
import no.unit.nva.search2.enums.ParameterKey;
import org.opensearch.common.unit.Fuzziness;
import org.opensearch.index.query.DisMaxQueryBuilder;
import org.opensearch.index.query.Operator;
import org.opensearch.index.query.QueryBuilder;
import org.opensearch.index.query.QueryBuilders;
import org.opensearch.index.query.TermsQueryBuilder;

public class OpensearchQueryFuzzyKeyword<K extends Enum<K> & ParameterKey> extends OpensearchQuery<K> {

    @Override
    protected Stream<Map.Entry<K, QueryBuilder>> queryAsEntryStream(K key, String... values) {
       return buildMatchAnyKeyValueQuery(key, values);
    }

    private Stream<Map.Entry<K, QueryBuilder>> buildMatchAnyKeyValueQuery(K key, String... values) {
        var boolQuery = QueryBuilders.boolQuery()
            .should(buildMatchKeyword(key, values))
            .must(buildMatchFuzzy(key, values));
        return queryTools.queryToEntry(key, boolQuery);
    }

    private QueryBuilder buildMatchFuzzy(K key, String... values) {
        final var searchFields = queryTools.getSearchFields(key);
        return Arrays.stream(values)
            .map(value -> QueryBuilders
                .multiMatchQuery(value, searchFields)
                .fuzziness(Fuzziness.AUTO)
                .maxExpansions(10)
                .operator(Operator.OR))
            .collect(DisMaxQueryBuilder::new, DisMaxQueryBuilder::add, DisMaxQueryBuilder::add);

    }

    private QueryBuilder buildMatchKeyword(K key, String... values) {
       return Arrays.stream(queryTools.getSearchFields(key))
            .map(searchField -> new TermsQueryBuilder(searchField, values).boost(key.fieldBoost()))
            .collect(DisMaxQueryBuilder::new, DisMaxQueryBuilder::add, DisMaxQueryBuilder::add);
    }


}