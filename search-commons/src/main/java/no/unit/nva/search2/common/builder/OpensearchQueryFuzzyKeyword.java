package no.unit.nva.search2.common.builder;

import static no.unit.nva.search2.common.constant.Words.KEYWORD_FALSE;
import static no.unit.nva.search2.common.constant.Words.KEYWORD_TRUE;
import java.util.Arrays;
import java.util.Map.Entry;
import java.util.stream.Stream;
import no.unit.nva.search2.common.enums.ParameterKey;
import org.opensearch.common.unit.Fuzziness;
import org.opensearch.index.query.DisMaxQueryBuilder;
import org.opensearch.index.query.Operator;
import org.opensearch.index.query.QueryBuilder;
import org.opensearch.index.query.QueryBuilders;
import org.opensearch.index.query.TermQueryBuilder;
import org.opensearch.index.query.TermsQueryBuilder;

public class OpensearchQueryFuzzyKeyword<K extends Enum<K> & ParameterKey> extends OpensearchQuery<K> {

    @Override
    protected Stream<Entry<K, QueryBuilder>> buildMatchAnyKeyValuesQuery(K key, String... values) {
        var boolQuery = QueryBuilders.boolQuery()
            .should(buildMatchAnyKeyword(key, values))
            .must(buildMatchAnyFuzzy(key, values))
            .queryName("FuzzyKeywordAny" + key.asCamelCase());
        return queryTools.queryToEntry(key, boolQuery);
    }

    @Override
    protected Stream<Entry<K, QueryBuilder>> buildMatchAllValuesQuery(K key, String... values) {
        var boolQuery = QueryBuilders.boolQuery()
            .queryName("FuzzyKeywordAll-" + key.asCamelCase());
        buildMatchAllKeyword(key, values).forEach(boolQuery::should);
        buildMatchAllFuzzy(key, values).forEach(boolQuery::must);

        return queryTools.queryToEntry(key, boolQuery);
    }


    private DisMaxQueryBuilder buildMatchAnyKeyword(K key, String... values) {
        return key.searchFields(KEYWORD_TRUE)
            .map(searchField -> new TermsQueryBuilder(searchField, values))
            .collect(DisMaxQueryBuilder::new, DisMaxQueryBuilder::add, DisMaxQueryBuilder::add)
            .boost(key.fieldBoost());
    }

    private DisMaxQueryBuilder buildMatchAnyFuzzy(K key, String... values) {
        return Arrays.stream(values)
            .map(value -> getMultiMatchQueryBuilder(value, key))
            .collect(DisMaxQueryBuilder::new, DisMaxQueryBuilder::add, DisMaxQueryBuilder::add);
    }

    private QueryBuilder getMultiMatchQueryBuilder(String value, K key) {
        final var searchFields = key.searchFields(KEYWORD_FALSE).toArray(String[]::new);
        return QueryBuilders
            .multiMatchQuery(value, searchFields)
            .fuzziness(Fuzziness.ZERO)
            .maxExpansions(10)
            .operator(Operator.AND);
    }

    private Stream<DisMaxQueryBuilder> buildMatchAllKeyword(K key, String... values) {
        return Arrays.stream(values)
            .map(value -> key.searchFields(KEYWORD_TRUE)
                .map(searchField -> new TermQueryBuilder(searchField, value))
                .collect(DisMaxQueryBuilder::new, DisMaxQueryBuilder::add, DisMaxQueryBuilder::add)
                .boost(key.fieldBoost())
            );
    }

    private Stream<DisMaxQueryBuilder> buildMatchAllFuzzy(K key, String... values) {
        return Arrays.stream(values)
            .map(singleValue -> getMatchPhrasePrefixBuilderStream(singleValue, key)
                .collect(DisMaxQueryBuilder::new, DisMaxQueryBuilder::add, DisMaxQueryBuilder::add)
            );
    }

    private Stream<QueryBuilder> getMatchPhrasePrefixBuilderStream(String singleValue, K key) {
        return key.searchFields(KEYWORD_FALSE)
            .map(fieldName -> QueryBuilders
                .matchPhrasePrefixQuery(fieldName, singleValue)
                .maxExpansions(10)
            );
    }
}