package no.unit.nva.search2.common.builder;

import java.util.Arrays;
import java.util.Map;
import java.util.Map.Entry;
import java.util.stream.Stream;
import no.unit.nva.search2.enums.ParameterKey;
import no.unit.nva.search2.enums.ParameterKey.FieldOperator;
import org.opensearch.index.query.DisMaxQueryBuilder;
import org.opensearch.index.query.MatchPhraseQueryBuilder;
import org.opensearch.index.query.QueryBuilder;
import org.opensearch.index.query.QueryBuilders;

public class OpensearchQueryText<K extends Enum<K> & ParameterKey> extends OpensearchQuery<K> {

    @Override
    protected Stream<Map.Entry<K, QueryBuilder>> valueQuery(K key, String value) {
        return buildAllMustMatchQuery(key, value);
    }

    @Override
    protected Stream<Map.Entry<K, QueryBuilder>> multiValueQuery(K key, String... values) {

        if (FieldOperator.SHOULD.equals(key.searchOperator())) {
            return buildShouldMatchQuery(key, values);
        } else {
            return buildAllMustMatchQuery(key, values);
        }
    }

    private Stream<Entry<K, QueryBuilder>> buildShouldMatchQuery(K key, String... values) {
        final var searchFields = getSearchFields(key);
        var builder = Arrays.stream(values)
            .flatMap(singleValue -> getMatchFraseBuilderStream(singleValue, searchFields))
            .collect(DisMaxQueryBuilder::new, DisMaxQueryBuilder::add, DisMaxQueryBuilder::add);
        return queryToEntry(key, builder);
    }

    private Stream<Entry<K, QueryBuilder>> buildAllMustMatchQuery(K key, String... values) {
        final var searchFields = getSearchFields(key);
        return Arrays.stream(values)
            .map(singleValue -> getMatchFraseBuilderStream(singleValue, searchFields)
                .collect(DisMaxQueryBuilder::new, DisMaxQueryBuilder::add, DisMaxQueryBuilder::add))
            .flatMap(builder -> queryToEntry(key, builder));
    }

    private Stream<MatchPhraseQueryBuilder> getMatchFraseBuilderStream(String singleValue, String... fieldNames) {
        return Arrays.stream(fieldNames)
            .map(fieldName -> QueryBuilders.matchPhraseQuery(fieldName, singleValue));
    }


}
