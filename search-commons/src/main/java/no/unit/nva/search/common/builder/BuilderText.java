package no.unit.nva.search.common.builder;

import static no.unit.nva.search.common.constant.Words.KEYWORD_FALSE;
import java.util.Arrays;
import java.util.Map.Entry;
import java.util.stream.Stream;

import no.unit.nva.search.common.constant.Functions;
import no.unit.nva.search.common.enums.ParameterKey;
import org.opensearch.index.query.DisMaxQueryBuilder;
import org.opensearch.index.query.Operator;
import org.opensearch.index.query.QueryBuilder;
import org.opensearch.index.query.QueryBuilders;

import static org.opensearch.index.query.QueryBuilders.matchPhrasePrefixQuery;
import static org.opensearch.index.query.QueryBuilders.matchQuery;

/**
 * @author Stig Norland
 */
public class BuilderText<K extends Enum<K> & ParameterKey> extends AbstractBuilder<K> {

    public static final String TEXT_ALL = "TextAll-";
    public static final String TEXT_ANY = "TextAny-";

    @Override
    protected Stream<Entry<K, QueryBuilder>> buildMatchAnyKeyValuesQuery(K key, String... values) {
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
            .map(singleValue -> phrasePrefixBuilder(singleValue, key)
                .collect(DisMaxQueryBuilder::new, DisMaxQueryBuilder::add, DisMaxQueryBuilder::add)
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
            key.searchFields(KEYWORD_FALSE).map(fieldName -> matchPhrasePrefixQuery(fieldName, singleValue)
                .boost(key.fieldBoost() + 0.1F)),
            key.searchFields(KEYWORD_FALSE).map(fieldName -> matchQuery(fieldName, singleValue)
                .operator(Operator.AND)
                .boost(key.fieldBoost())));
    }
}