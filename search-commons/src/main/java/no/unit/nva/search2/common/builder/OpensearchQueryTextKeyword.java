package no.unit.nva.search2.common.builder;

import java.util.Arrays;
import java.util.Map.Entry;
import java.util.stream.Stream;
import no.unit.nva.search2.common.enums.ParameterKey;
import nva.commons.core.JacocoGenerated;
import org.opensearch.index.query.DisMaxQueryBuilder;
import org.opensearch.index.query.QueryBuilder;
import org.opensearch.index.query.QueryBuilders;
import org.opensearch.index.query.TermQueryBuilder;
import org.opensearch.index.query.TermsQueryBuilder;

@JacocoGenerated    // not implemented yet....
public class OpensearchQueryTextKeyword<K extends Enum<K> & ParameterKey> extends OpensearchQuery<K> {

    @Override
    protected Stream<Entry<K, QueryBuilder>> buildMatchAnyKeyValuesQuery(K key, String... values) {
        return buildAnyComboMustHitQuery(key, values)
            .flatMap(builder -> queryTools.queryToEntry(key, builder));
    }

    @Override
    protected Stream<Entry<K, QueryBuilder>> buildMatchAllValuesQuery(K key, String... values) {
        return buildEachValueMustHitQuery(key, values)
            .flatMap(builder -> queryTools.queryToEntry(key, builder));
    }

    private Stream<QueryBuilder> buildEachValueMustHitQuery(K key, String... values) {
        return Arrays.stream(values)
            .flatMap(singleValue -> buildAnyComboMustHitQuery(key, singleValue));
    }

    private Stream<DisMaxQueryBuilder> buildAnyComboMustHitQuery(K key, String... values) {
        var disMax = QueryBuilders.disMaxQuery();
        Arrays.stream(queryTools.getSearchFields(key))
            .forEach(field -> disMax.add(new TermsQueryBuilder(field, values).boost(key.fieldBoost()))
            );
        return Stream.of(disMax);
    }

    private Stream<DisMaxQueryBuilder> buildAnyComboMustHitQuery(K key, String value) {
        var disMax = QueryBuilders.disMaxQuery();
        Arrays.stream(queryTools.getSearchFields(key))
            .forEach(field -> disMax.add(new TermQueryBuilder(field, value).boost(key.fieldBoost()))
            );
        return Stream.of(disMax);
    }
}