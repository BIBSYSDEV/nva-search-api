package no.unit.nva.search2.common.builder;

import no.unit.nva.search2.common.ParameterKey;
import org.opensearch.index.query.QueryBuilder;
import org.opensearch.index.query.TermQueryBuilder;
import org.opensearch.index.query.TermsQueryBuilder;

import java.util.Arrays;
import java.util.Map;
import java.util.stream.Stream;

public class OpensearchQueryKeyword<K extends Enum<K> & ParameterKey> extends OpensearchQuery<K> {

    @Override
    protected Stream<Map.Entry<K, QueryBuilder>> valueQuery(K key, String value) {
        return queryToEntry(key, new TermQueryBuilder(getFirstSearchField(key), value));
    }

    @Override
    protected Stream<Map.Entry<K, QueryBuilder>> multiValueQuery(K key, String... values) {
        if (isOperatorAnd(key)) {
            return buildEachValueMustHitQuery(key, values)
                .flatMap(builder -> queryToEntry(key, builder));
        } else {
            return queryToEntry(key, new TermsQueryBuilder(getFirstSearchField(key), values));
        }
    }


    private Stream<QueryBuilder> buildEachValueMustHitQuery(K key, String... values) {
        return Arrays.stream(values)
            .map(singleValue -> new TermQueryBuilder(getFirstSearchField(key), singleValue));
    }

}
