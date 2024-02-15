package no.unit.nva.search2.common.builder;

import java.util.Arrays;
import java.util.Map.Entry;
import java.util.stream.Stream;
import no.unit.nva.search2.common.enums.ParameterKey;
import org.opensearch.index.query.BoolQueryBuilder;
import org.opensearch.index.query.DisMaxQueryBuilder;
import org.opensearch.index.query.QueryBuilder;
import org.opensearch.index.query.TermQueryBuilder;
import org.opensearch.index.query.TermsQueryBuilder;

public class OpensearchQueryKeyword<K extends Enum<K> & ParameterKey> extends OpensearchQuery<K> {

    @Override
    protected Stream<Entry<K, QueryBuilder>> buildMatchAnyKeyValuesQuery(K key, String... values) {
        return queryTools.queryToEntry(key, buildMatchAnyKeyword(key, values)
            .queryName("KeywordAny-" + key.name())
        );
    }

    @Override
    protected Stream<Entry<K, QueryBuilder>> buildMatchAllValuesQuery(K key, String... values) {
        return queryTools.queryToEntry(key, buildMatchAllKeyword(key, values)
            .queryName("KeywordAll-" + key.name())
        );
    }

    private QueryBuilder buildMatchAnyKeyword(K key, String... values) {
        return key.searchFields().stream()
            .map(searchField -> new TermsQueryBuilder(searchField, values))
            .collect(DisMaxQueryBuilder::new, DisMaxQueryBuilder::add, DisMaxQueryBuilder::add);
    }

    private QueryBuilder buildMatchAllKeyword(K key, String... values) {
        return key.searchFields().stream()
            .flatMap(searchField ->
                         Arrays.stream(values)
                             .map(value -> new TermQueryBuilder(searchField, value)))
            .collect(BoolQueryBuilder::new, BoolQueryBuilder::must, BoolQueryBuilder::must);
    }
}
