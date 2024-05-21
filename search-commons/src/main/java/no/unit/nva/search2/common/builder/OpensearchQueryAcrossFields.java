package no.unit.nva.search2.common.builder;

import static no.unit.nva.search2.common.constant.Words.KEYWORD_FALSE;

import java.util.Arrays;
import java.util.Map.Entry;
import java.util.stream.Stream;

import no.unit.nva.search2.common.constant.Functions;
import no.unit.nva.search2.common.enums.ParameterKey;
import org.opensearch.index.query.BoolQueryBuilder;
import org.opensearch.index.query.MultiMatchQueryBuilder;
import org.opensearch.index.query.Operator;
import org.opensearch.index.query.QueryBuilder;
import org.opensearch.index.query.QueryBuilders;

public class OpensearchQueryAcrossFields<K extends Enum<K> & ParameterKey> extends OpensearchQuery<K> {

    @Override
    protected Stream<Entry<K, QueryBuilder>> buildMatchAnyKeyValuesQuery(K key, String... values) {
        return Functions.queryToEntry(key,
            buildMultiMatchQueryStream(key, values)
                .collect(BoolQueryBuilder::new, BoolQueryBuilder::should, BoolQueryBuilder::should)
                .queryName("AcrossFieldsAnyValue" + key.asCamelCase()));
    }

    @Override
    protected Stream<Entry<K, QueryBuilder>> buildMatchAllValuesQuery(K key, String... values) {
        return Functions.queryToEntry(key,
            buildMultiMatchQueryStream(key, values)
                .collect(BoolQueryBuilder::new, BoolQueryBuilder::must, BoolQueryBuilder::must)
                .queryName("AcrossFieldsAllValues" + key.asCamelCase()));
    }

    private Stream<QueryBuilder> buildMultiMatchQueryStream(K key, String... values) {
        return Arrays.stream(values)
            .map(singleValue -> getMultiMatchQueryBuilder(singleValue, key));
    }

    private QueryBuilder getMultiMatchQueryBuilder(String value, K key) {
        final var searchFields = key.searchFields(KEYWORD_FALSE).toArray(String[]::new);
        return QueryBuilders
            .multiMatchQuery(value, searchFields)
            .type(MultiMatchQueryBuilder.Type.CROSS_FIELDS)
            .operator(Operator.AND);
    }
}