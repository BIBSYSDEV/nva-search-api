package no.unit.nva.search.model.builder;

import static no.unit.nva.search.model.constant.Functions.queryToEntry;
import static no.unit.nva.search.model.constant.Words.KEYWORD_FALSE;

import no.unit.nva.search.model.enums.ParameterKey;

import nva.commons.core.JacocoGenerated;

import org.opensearch.index.query.BoolQueryBuilder;
import org.opensearch.index.query.ExistsQueryBuilder;
import org.opensearch.index.query.QueryBuilder;
import org.opensearch.index.query.QueryBuilders;

import java.util.Map.Entry;
import java.util.stream.Stream;

/**
 * Class for building OpenSearch queries that check if a field exists.
 *
 * @author Stig Norland
 * @param <K> the type of the parameter keys used in the query. The parameter keys are used to
 *     define the parameters that can be used in the query.
 */
public class ExistsQuery<K extends Enum<K> & ParameterKey<K>> extends AbstractBuilder<K> {

    public static final String EXISTS_ANY = "ExistsAny-";

    @Override
    protected Stream<Entry<K, QueryBuilder>> buildMatchAnyValueQuery(K key, String... values) {
        return buildExistsQuery(key, Boolean.valueOf(values[0]));
    }

    @Override
    @JacocoGenerated // not currently in use...
    protected Stream<Entry<K, QueryBuilder>> buildMatchAllValuesQuery(K key, String... values) {
        return buildExistsQuery(key, Boolean.valueOf(values[0]));
    }

    private Stream<Entry<K, QueryBuilder>> buildExistsQuery(K key, Boolean exists) {
        var builder = QueryBuilders.boolQuery().queryName(EXISTS_ANY);
        key.searchFields(KEYWORD_FALSE)
                .map(fieldName -> createExistsQuery(key, fieldName))
                .forEach(existsQueryBuilder -> mustOrNot(exists, builder, existsQueryBuilder));
        return queryToEntry(key, builder);
    }

    private ExistsQueryBuilder createExistsQuery(K key, String fieldName) {
        return QueryBuilders.existsQuery(fieldName).boost(key.fieldBoost());
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
