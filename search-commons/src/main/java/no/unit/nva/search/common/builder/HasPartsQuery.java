package no.unit.nva.search.common.builder;

import java.util.Map;
import java.util.stream.Stream;
import no.unit.nva.search.common.constant.Functions;
import no.unit.nva.search.common.enums.ParameterKey;
import org.apache.lucene.search.join.ScoreMode;
import org.opensearch.index.query.QueryBuilder;
import org.opensearch.index.query.QueryBuilders;
import org.opensearch.join.query.HasChildQueryBuilder;

public class HasPartsQuery<K extends Enum<K> & ParameterKey> extends AbstractBuilder<K> {

    @Override
    protected Stream<Map.Entry<K, QueryBuilder>> buildMatchAnyKeyValuesQuery(K key, String... values) {
        return buildAnyComboMustHitQuery(key, values)
            .flatMap(builder -> Functions.queryToEntry(key, builder));
    }

    @Override
    protected Stream<Map.Entry<K, QueryBuilder>> buildMatchAllValuesQuery(K key, String... values) {
        return buildAllMustHitQuery(key, values)
            .flatMap(builder -> Functions.queryToEntry(key, builder));
    }


    private Stream<QueryBuilder> buildAllMustHitQuery(K key, String... values) {
    var builder =
        new HasChildQueryBuilder("partOf", getSubQuery((K) key.subquery(), values), ScoreMode.None);
        return Stream.of(builder);
    }

    private Stream<QueryBuilder> buildAnyComboMustHitQuery(K key, String... values) {
    var builder =
        new HasChildQueryBuilder("partOf", getSubQuery((K) key.subquery(), values), ScoreMode.None);
        return Stream.of(builder);

    }


    private QueryBuilder getSubQuery(K key, String... values) {
        return
            switch (key.fieldType()) {
            case KEYWORD -> new KeywordQuery<K>().buildQuery(key,values).findFirst().orElseThrow().getValue();
            case FUZZY_KEYWORD -> new FuzzyKeywordQuery<K>().buildQuery(key,values).findFirst().orElseThrow().getValue();
            case TEXT -> new TextQuery<K>().buildQuery(key,values).findFirst().orElseThrow().getValue();
            case ACROSS_FIELDS -> new AcrossFieldsQuery<K>().buildQuery(key,values).findFirst().orElseThrow().getValue();
                case FREE_TEXT -> QueryBuilders.matchAllQuery();
            default -> throw new IllegalStateException("Unexpected value: " + key.fieldType());
        };
    }

}
