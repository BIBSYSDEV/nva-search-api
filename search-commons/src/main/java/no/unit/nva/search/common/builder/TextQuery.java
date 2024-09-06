package no.unit.nva.search.common.builder;

import static no.unit.nva.constants.Words.KEYWORD_FALSE;

import static org.opensearch.index.query.QueryBuilders.matchPhrasePrefixQuery;
import static org.opensearch.index.query.QueryBuilders.matchQuery;

import no.unit.nva.search.common.constant.Functions;
import no.unit.nva.search.common.enums.ParameterKey;

import org.opensearch.index.query.DisMaxQueryBuilder;
import org.opensearch.index.query.MatchPhrasePrefixQueryBuilder;
import org.opensearch.index.query.MatchQueryBuilder;
import org.opensearch.index.query.Operator;
import org.opensearch.index.query.QueryBuilder;
import org.opensearch.index.query.QueryBuilders;

import java.util.Arrays;
import java.util.Map.Entry;
import java.util.stream.Stream;

/**
 * @author Stig Norland
 */
public class TextQuery<K extends Enum<K> & ParameterKey<K>> extends AbstractBuilder<K> {

    public final String TEXT_ALL = "TextAll-";
    public final String TEXT_ANY = "TextAny-";

    @Override
    protected Stream<Entry<K, QueryBuilder>> buildMatchAnyValueQuery(K key, String... values) {
        return buildAnyComboMustHitQuery(key, values)
                .flatMap(builder -> Functions.queryToEntry(key, builder));
    }

    @Override
    protected Stream<Entry<K, QueryBuilder>> buildMatchAllValuesQuery(K key, String... values) {
        return buildAllMustHitQuery(key, values)
                .flatMap(builder -> Functions.queryToEntry(key, builder));
    }

    private Stream<QueryBuilder> buildAllMustHitQuery(K key, String... values) {
        return Arrays.stream(values)
                .map(
                        singleValue ->
                                phrasePrefixBuilder(singleValue, key)
                                        .collect(
                                                DisMaxQueryBuilder::new,
                                                DisMaxQueryBuilder::add,
                                                DisMaxQueryBuilder::add)
                                        .queryName(TEXT_ALL + key.asCamelCase()));
    }

    private Stream<DisMaxQueryBuilder> buildAnyComboMustHitQuery(K key, String... values) {
        var disMax = QueryBuilders.disMaxQuery().queryName(TEXT_ANY + key.asCamelCase());
        Arrays.stream(values)
                .flatMap(singleValue -> phrasePrefixBuilder(singleValue, key))
                .forEach(disMax::add);
        return Stream.of(disMax);
    }

    private Stream<QueryBuilder> phrasePrefixBuilder(String singleValue, K key) {
        return Stream.concat(
                key.searchFields(KEYWORD_FALSE)
                        .map(fieldName -> matchPhrasePrefixBuilder(singleValue, key, fieldName)),
                key.searchFields(KEYWORD_FALSE)
                        .map(fieldName -> matchQueryBuilder(singleValue, key, fieldName)));
    }

    private MatchQueryBuilder matchQueryBuilder(String singleValue, K key, String fieldName) {
        return matchQuery(fieldName, singleValue).operator(Operator.AND).boost(key.fieldBoost());
    }

    private MatchPhrasePrefixQueryBuilder matchPhrasePrefixBuilder(
            String singleValue, K key, String fieldName) {
        return matchPhrasePrefixQuery(fieldName, singleValue).boost(key.fieldBoost() + 0.1F);
    }
}
