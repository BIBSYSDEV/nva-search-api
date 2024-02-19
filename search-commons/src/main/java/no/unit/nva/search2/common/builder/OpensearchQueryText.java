package no.unit.nva.search2.common.builder;

import java.util.Arrays;
import java.util.Map.Entry;
import java.util.stream.Stream;
import no.unit.nva.search2.common.enums.ParameterKey;
import org.opensearch.index.query.BoolQueryBuilder;
import org.opensearch.index.query.DisMaxQueryBuilder;
import org.opensearch.index.query.MatchPhrasePrefixQueryBuilder;
import org.opensearch.index.query.QueryBuilder;
import org.opensearch.index.query.QueryBuilders;

public class OpensearchQueryText<K extends Enum<K> & ParameterKey> extends OpensearchQuery<K> {

    @Override
    protected Stream<Entry<K, QueryBuilder>> buildMatchAnyKeyValuesQuery(K key, String... values) {
        final var searchFields = queryTools.getSearchFields(key);
        return queryTools.queryToEntry(key, Arrays.stream(values)
            .flatMap(singleValue -> getMatchPhrasePrefixBuilderStream(singleValue, searchFields))
            .collect(DisMaxQueryBuilder::new, DisMaxQueryBuilder::add, DisMaxQueryBuilder::add)
            .queryName("TextAny-" + key.fieldName()));
    }

    @Override
    protected Stream<Entry<K, QueryBuilder>> buildMatchAllValuesQuery(K key, String... values) {
        final var searchFields = queryTools.getSearchFields(key);
        return queryTools.queryToEntry(key, Arrays.stream(values)
            .map(singleValue ->
                     getMatchPhrasePrefixBuilderStream(singleValue, searchFields)
                         .collect(DisMaxQueryBuilder::new, DisMaxQueryBuilder::add, DisMaxQueryBuilder::add)
            )
            .collect(BoolQueryBuilder::new, BoolQueryBuilder::must, BoolQueryBuilder::must)
            .queryName("TextAll-" + key.fieldName()));
    }

    private Stream<MatchPhrasePrefixQueryBuilder> getMatchPhrasePrefixBuilderStream(String singleValue,
                                                                                    String... fieldNames) {
        return Arrays.stream(fieldNames)
            .map(fieldName -> QueryBuilders.matchPhrasePrefixQuery(fieldName, singleValue));
    }
}