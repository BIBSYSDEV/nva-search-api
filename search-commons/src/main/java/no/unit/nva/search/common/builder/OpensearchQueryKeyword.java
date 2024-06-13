package no.unit.nva.search.common.builder;

import java.util.Arrays;
import java.util.Map.Entry;
import java.util.stream.Stream;

import static no.unit.nva.search.common.constant.Words.KEYWORD_TRUE;

import no.unit.nva.search.common.constant.Functions;
import no.unit.nva.search.common.enums.ParameterKey;
import org.opensearch.index.query.DisMaxQueryBuilder;
import org.opensearch.index.query.QueryBuilder;
import org.opensearch.index.query.QueryBuilders;
import org.opensearch.index.query.TermQueryBuilder;
import org.opensearch.index.query.TermsQueryBuilder;

/**
 * @author Stig Norland
 */
public class OpensearchQueryKeyword<K extends Enum<K> & ParameterKey> extends OpensearchQuery<K> {

    @Override
    protected Stream<Entry<K, QueryBuilder>> buildMatchAnyKeyValuesQuery(K key, String... values) {
        return buildMatchAnyKeywordStream(key, values)
            .flatMap(builder -> Functions.queryToEntry(key, builder));
    }

    @Override
    protected Stream<Entry<K, QueryBuilder>> buildMatchAllValuesQuery(K key, String... values) {
        return buildMatchAllKeywordStream(key, values)
            .flatMap(builder -> Functions.queryToEntry(key, builder));
    }

    private Stream<QueryBuilder> buildMatchAllKeywordStream(K key, String... values) {
        return Arrays.stream(values)
            .flatMap(value -> key.searchFields(KEYWORD_TRUE)
                .map(searchField -> new TermQueryBuilder(searchField, value).queryName(
                    "KeywordAll-" + key.asCamelCase())));
    }

    private Stream<DisMaxQueryBuilder> buildMatchAnyKeywordStream(K key, String... values) {
        var disMax = QueryBuilders.disMaxQuery().queryName("KeywordAny-" + key.asCamelCase());
        key.searchFields(KEYWORD_TRUE)
            .forEach(field -> disMax.add(new TermsQueryBuilder(field, values).boost(key.fieldBoost())));
        return Stream.of(disMax);
    }
}
