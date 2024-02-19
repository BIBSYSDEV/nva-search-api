package no.unit.nva.search2.common.builder;

import java.util.Arrays;
import java.util.Map.Entry;
import java.util.stream.Stream;
import no.unit.nva.search2.common.enums.ParameterKey;
import org.opensearch.common.unit.Fuzziness;
import org.opensearch.index.query.BoolQueryBuilder;
import org.opensearch.index.query.DisMaxQueryBuilder;
import org.opensearch.index.query.MatchPhrasePrefixQueryBuilder;
import org.opensearch.index.query.MultiMatchQueryBuilder;
import org.opensearch.index.query.Operator;
import org.opensearch.index.query.QueryBuilder;
import org.opensearch.index.query.QueryBuilders;
import org.opensearch.index.query.TermQueryBuilder;
import org.opensearch.index.query.TermsQueryBuilder;

public class OpensearchQueryFuzzyKeyword<K extends Enum<K> & ParameterKey> extends OpensearchQuery<K> {

    @Override
    protected Stream<Entry<K, QueryBuilder>> buildMatchAnyKeyValuesQuery(K key, String... values) {
        var boolQuery = QueryBuilders.boolQuery()
            .should(buildMatchAnyKeyword(key, values).boost(key.fieldBoost()))
            .must(buildMatchAnyFuzzy(key, values))
            .queryName("FuzzyKeywordAny" + key.name());
        return queryTools.queryToEntry(key, boolQuery);
    }

    @Override
    protected Stream<Entry<K, QueryBuilder>> buildMatchAllValuesQuery(K key, String... values) {
        var boolQuery = QueryBuilders.boolQuery()
            .should(buildMatchAllKeyword(key, values).boost(key.fieldBoost()))
            .must(buildMatchAllFuzzy(key, values))
            .queryName("FuzzyKeywordAll-" + key.fieldName());
        return queryTools.queryToEntry(key, boolQuery);
    }

    private QueryBuilder buildMatchAnyFuzzy(K key, String... values) {
        final var searchFields = queryTools.getSearchFields(key);
        return Arrays.stream(values)
            .map(value -> getMultiMatchQueryBuilder(value, searchFields))
            .collect(DisMaxQueryBuilder::new, DisMaxQueryBuilder::add, DisMaxQueryBuilder::add);
    }

    private QueryBuilder buildMatchAnyKeyword(K key, String... values) {
        return key.searchFields().stream()
            .map(searchField -> new TermsQueryBuilder(searchField, values))
            .collect(DisMaxQueryBuilder::new, DisMaxQueryBuilder::add, DisMaxQueryBuilder::add);
    }

    private QueryBuilder buildMatchAllFuzzy(K key, String... values) {
        final var searchFields = queryTools.getSearchFields(key);
        return Arrays.stream(values)
            .map(singleValue ->
                     getMatchPhrasePrefixBuilderStream(singleValue, searchFields)
                         .collect(DisMaxQueryBuilder::new, DisMaxQueryBuilder::add, DisMaxQueryBuilder::add)
            )
            .collect(BoolQueryBuilder::new, BoolQueryBuilder::must, BoolQueryBuilder::must);
    }

    private QueryBuilder buildMatchAllKeyword(K key, String... values) {
        return key.searchFields().stream()
            .flatMap(searchField ->
                         Arrays.stream(values)
                             .map(value -> new TermQueryBuilder(searchField, value)))
            .collect(BoolQueryBuilder::new, BoolQueryBuilder::must, BoolQueryBuilder::must);
    }

    private static MultiMatchQueryBuilder getMultiMatchQueryBuilder(String value, String... searchFields) {
        return QueryBuilders
            .multiMatchQuery(value, searchFields)
            .fuzziness(Fuzziness.AUTO)
            .maxExpansions(10)
            .operator(Operator.AND);
    }

    private Stream<MatchPhrasePrefixQueryBuilder> getMatchPhrasePrefixBuilderStream(
        String singleValue, String... fieldNames) {

        return Arrays.stream(fieldNames)
            .map(fieldName -> QueryBuilders
                .matchPhrasePrefixQuery(fieldName, singleValue)
                .maxExpansions(10)
            );
    }

}