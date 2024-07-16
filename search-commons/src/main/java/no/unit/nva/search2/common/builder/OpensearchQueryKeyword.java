package no.unit.nva.search2.common.builder;

import static no.unit.nva.search2.common.constant.Words.KEYWORD_TRUE;

import java.util.Arrays;
import java.util.Map.Entry;
import java.util.stream.Stream;
import no.unit.nva.search2.common.constant.Functions;
import no.unit.nva.search2.common.enums.ParameterKey;
import org.opensearch.index.query.DisMaxQueryBuilder;
import org.opensearch.index.query.QueryBuilder;
import org.opensearch.index.query.QueryBuilders;
import org.opensearch.index.query.TermQueryBuilder;
import org.opensearch.index.query.TermsQueryBuilder;

/**
 * @author Stig Norland
 */
public class OpensearchQueryKeyword<K extends Enum<K> & ParameterKey> extends OpensearchQuery<K> {

    public static final String KEYWORD_ALL = "KeywordAll-";
    public static final String KEYWORD_ANY = "KeywordAny-";

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
                    KEYWORD_ALL + key.asCamelCase())));
    }

    private Stream<DisMaxQueryBuilder> buildMatchAnyKeywordStream(K key, String... values) {
        var disMax = QueryBuilders.disMaxQuery().queryName(KEYWORD_ANY + key.asCamelCase());
        key.searchFields(KEYWORD_TRUE)
            .forEach(field -> disMax.add(new TermsQueryBuilder(field, values).boost(key.fieldBoost())));
        return Stream.of(disMax);
    }
}
