package no.unit.nva.search.common.builder;

import no.unit.nva.search.common.enums.ParameterKey;
import org.opensearch.index.query.BoolQueryBuilder;
import org.opensearch.index.query.ExistsQueryBuilder;
import org.opensearch.index.query.QueryBuilder;

import java.util.Map.Entry;
import java.util.function.BiConsumer;
import java.util.function.Supplier;
import java.util.stream.Stream;

import static no.unit.nva.search.common.constant.Functions.queryToEntry;
import static no.unit.nva.search.common.constant.Words.KEYWORD_FALSE;
import static org.opensearch.index.query.QueryBuilders.boolQuery;
import static org.opensearch.index.query.QueryBuilders.existsQuery;

/**
 * @author Stig Norland
 */
public class OpensearchQueryExists<K extends Enum<K> & ParameterKey> extends OpensearchQuery<K> {

    public static final String EXISTS_ANY = "ExistsAny-";

    @Override
    protected Stream<Entry<K, QueryBuilder>> buildMatchAnyKeyValuesQuery(K key, String... values) {
        return buildAnyComboMustHitQuery(key, Boolean.valueOf(values[0]));
    }

    @Override
    protected Stream<Entry<K, QueryBuilder>> buildMatchAllValuesQuery(K key, String... values) {
        return buildAnyComboMustHitQuery(key, Boolean.valueOf(values[0]));
    }

    private Stream<Entry<K, QueryBuilder>> buildAnyComboMustHitQuery(K key, Boolean exists) {
        return queryToEntry(key,
            key.searchFields(KEYWORD_FALSE)
                .map(fieldName -> createExistsQuery(key, fieldName))
                .collect(createBuilder(), addQuery(exists), mergeQuery(exists))
        );
    }

    private ExistsQueryBuilder createExistsQuery(K key, String fieldName) {
        return existsQuery(fieldName)
            .boost(key.fieldBoost());
    }

    private Supplier<BoolQueryBuilder> createBuilder() {
        return () -> boolQuery().queryName(EXISTS_ANY);
    }

    private BiConsumer<BoolQueryBuilder, BoolQueryBuilder> mergeQuery(Boolean exists) {
        return (boolQueryBuilder, queryBuilder) -> mustOrNot(exists, boolQueryBuilder, queryBuilder);
    }

    private BiConsumer<BoolQueryBuilder, ExistsQueryBuilder> addQuery(Boolean exists) {
        return (boolQueryBuilder, queryBuilder) -> mustOrNot(exists, boolQueryBuilder, queryBuilder);
    }

    private void mustOrNot(Boolean exists, BoolQueryBuilder boolQueryBuilder, QueryBuilder queryBuilder) {
        if (exists) {
            boolQueryBuilder.must(queryBuilder);
        } else {
            boolQueryBuilder.mustNot(queryBuilder);
        }
    }

}