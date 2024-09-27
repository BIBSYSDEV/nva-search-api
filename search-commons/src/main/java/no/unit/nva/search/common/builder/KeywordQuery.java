package no.unit.nva.search.common.builder;

import static no.unit.nva.constants.Words.KEYWORD_TRUE;

import no.unit.nva.search.common.constant.Functions;
import no.unit.nva.search.common.enums.ParameterKey;

import org.opensearch.index.query.DisMaxQueryBuilder;
import org.opensearch.index.query.QueryBuilder;
import org.opensearch.index.query.QueryBuilders;
import org.opensearch.index.query.TermQueryBuilder;
import org.opensearch.index.query.TermsQueryBuilder;

import java.util.Arrays;
import java.util.Map.Entry;
import java.util.stream.Stream;

/**
 * Class for building OpenSearch queries that search for keywords.
 *
 * @author Stig Norland
 */
public class KeywordQuery<K extends Enum<K> & ParameterKey<K>> extends AbstractBuilder<K> {

    public static final String KEYWORD_ALL = "KeywordAll-";
    public static final String KEYWORD_ANY = "KeywordAny-";

    private static <K extends Enum<K> & ParameterKey<K>> TermQueryBuilder getTermQueryBuilder(
            K key, String value, String searchField) {
        return new TermQueryBuilder(searchField, value).queryName(KEYWORD_ALL + key.asCamelCase());
    }

    @Override
    protected Stream<Entry<K, QueryBuilder>> buildMatchAnyValueQuery(K key, String... values) {
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
                .flatMap(
                        value ->
                                key.searchFields(KEYWORD_TRUE)
                                        .map(
                                                searchField ->
                                                        getTermQueryBuilder(
                                                                key, value, searchField)));
    }

    private Stream<DisMaxQueryBuilder> buildMatchAnyKeywordStream(K key, String... values) {
        var disMax = QueryBuilders.disMaxQuery().queryName(KEYWORD_ANY + key.asCamelCase());
        key.searchFields(KEYWORD_TRUE)
                .forEach(
                        field ->
                                disMax.add(
                                        new TermsQueryBuilder(field, values)
                                                .boost(key.fieldBoost())));
        return Stream.of(disMax);
    }
}
