package no.unit.nva.search2.common.builder;

import java.util.Arrays;
import java.util.Map;
import java.util.stream.Stream;
import no.unit.nva.search2.enums.ParameterKey;
import no.unit.nva.search2.enums.ParameterKey.FieldOperator;
import org.opensearch.index.query.DisMaxQueryBuilder;
import org.opensearch.index.query.QueryBuilder;
import org.opensearch.index.query.QueryBuilders;
import org.opensearch.index.query.TermQueryBuilder;
import org.opensearch.index.query.TermsQueryBuilder;

public class OpensearchQueryKeyword<K extends Enum<K> & ParameterKey> extends OpensearchQuery<K> {

    @Override
    protected Stream<Map.Entry<K, QueryBuilder>> valueQuery(K key, String value) {
        return buildEachValueMustHitQuery(key, value)
            .flatMap(builder -> queryToEntry(key, builder));
    }

    @Override
    protected Stream<Map.Entry<K, QueryBuilder>> multiValueQuery(K key, String... values) {

        if (FieldOperator.SHOULD.equals(key.searchOperator())) {
            return buildAnyComboMustHitQuery(key, values)
                .flatMap(builder -> queryToEntry(key, builder));
        } else {
            return buildEachValueMustHitQuery(key, values)
                .flatMap(builder -> queryToEntry(key, builder));
        }
    }

    private Stream<QueryBuilder> buildEachValueMustHitQuery(K key, String... values) {
        return Arrays.stream(values)
            .flatMap(singleValue -> buildAnyComboMustHitQuery(key, singleValue));
    }

    private Stream<DisMaxQueryBuilder> buildAnyComboMustHitQuery(K key, String... values) {
        var disMax = QueryBuilders.disMaxQuery();
        Arrays.stream(getSearchFields(key))
            .forEach(field -> disMax.add(new TermsQueryBuilder(field, values))
            );
        return Stream.of(disMax);
    }

    private Stream<DisMaxQueryBuilder> buildAnyComboMustHitQuery(K key, String value) {
        var disMax = QueryBuilders.disMaxQuery();
        Arrays.stream(getSearchFields(key))
            .forEach(field -> disMax.add(new TermQueryBuilder(field, value))
            );
        return Stream.of(disMax);
    }

}
