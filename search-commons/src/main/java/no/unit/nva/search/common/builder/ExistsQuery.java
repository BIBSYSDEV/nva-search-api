package no.unit.nva.search.common.builder;

import static no.unit.nva.search.common.constant.Functions.queryToEntry;
import static no.unit.nva.search.common.constant.Words.KEYWORD_FALSE;

import static org.opensearch.index.query.QueryBuilders.boolQuery;
import static org.opensearch.index.query.QueryBuilders.existsQuery;

import no.unit.nva.search.common.enums.ParameterKey;

import nva.commons.core.JacocoGenerated;

import org.opensearch.index.query.BoolQueryBuilder;
import org.opensearch.index.query.ExistsQueryBuilder;
import org.opensearch.index.query.QueryBuilder;

import java.util.Map.Entry;
import java.util.function.BiConsumer;
import java.util.function.Supplier;
import java.util.stream.Stream;

/**
 * @author Stig Norland
 */
public class ExistsQuery<K extends Enum<K> & ParameterKey<K>> extends AbstractBuilder<K> {

    public static final String EXISTS_ANY = "ExistsAny-";

    @Override
    protected Stream<Entry<K, QueryBuilder>> buildMatchAnyKeyValuesQuery(K key, String... values) {
        return buildAnyComboMustHitQuery(key, Boolean.valueOf(values[0]));
    }

    @JacocoGenerated // not in use
    @Override
    protected Stream<Entry<K, QueryBuilder>> buildMatchAllValuesQuery(K key, String... values) {
        return buildAnyComboMustHitQuery(key, Boolean.valueOf(values[0]));
    }

    private Stream<Entry<K, QueryBuilder>> buildAnyComboMustHitQuery(K key, Boolean exists) {
        return queryToEntry(
                key,
                key.searchFields(KEYWORD_FALSE)
                        .map(fieldName -> createExistsQuery(key, fieldName))
                        .collect(createBuilder(), addQuery(exists), mergeQuery(exists)));
    }

    private ExistsQueryBuilder createExistsQuery(K key, String fieldName) {
        return existsQuery(fieldName).boost(key.fieldBoost());
    }

    private Supplier<BoolQueryBuilder> createBuilder() {
        return () -> boolQuery().queryName(EXISTS_ANY);
    }

    @JacocoGenerated // not currently in use...
    private BiConsumer<BoolQueryBuilder, BoolQueryBuilder> mergeQuery(Boolean exists) {
        return (boolQueryBuilder, queryBuilder) ->
                mustOrNot(exists, boolQueryBuilder, queryBuilder);
    }

    private BiConsumer<BoolQueryBuilder, ExistsQueryBuilder> addQuery(Boolean exists) {
        return (boolQueryBuilder, queryBuilder) ->
                mustOrNot(exists, boolQueryBuilder, queryBuilder);
    }

    private void mustOrNot(
            Boolean exists, BoolQueryBuilder boolQueryBuilder, QueryBuilder queryBuilder) {
        if (exists) {
            boolQueryBuilder.should(queryBuilder);
            boolQueryBuilder.minimumShouldMatch(1);
        } else {
            boolQueryBuilder.mustNot(queryBuilder);
        }
    }
}
