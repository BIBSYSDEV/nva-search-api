package no.unit.nva.search.common.builder;

import static no.unit.nva.constants.Words.EXCLUDE_KEYWORD;

import java.util.Arrays;
import java.util.Map.Entry;
import java.util.stream.Stream;
import no.unit.nva.search.common.constant.Functions;
import no.unit.nva.search.common.enums.ParameterKey;
import org.opensearch.index.query.BoolQueryBuilder;
import org.opensearch.index.query.MultiMatchQueryBuilder;
import org.opensearch.index.query.Operator;
import org.opensearch.index.query.QueryBuilder;
import org.opensearch.index.query.QueryBuilders;

/**
 * Class for building OpenSearch queries that search across multiple fields.
 *
 * @author Stig Norland
 * @param <K> the type of the parameter keys used in the query. The parameter keys are used to
 *     define the parameters that can be used in the query.
 */
public class AcrossFieldsQuery<K extends Enum<K> & ParameterKey<K>> extends AbstractBuilder<K> {

  public static final String ANY_VALUE = "AcrossFieldsAnyValue";
  public static final String ALL_VALUES = "AcrossFieldsAllValues";

  @Override
  protected Stream<Entry<K, QueryBuilder>> buildMatchAnyValueQuery(K key, String... values) {
    return Functions.queryToEntry(
        key,
        buildMultiMatchQueryStream(key, values)
            .collect(BoolQueryBuilder::new, BoolQueryBuilder::should, BoolQueryBuilder::should)
            .minimumShouldMatch(1)
            .queryName(ANY_VALUE + key.asCamelCase()));
  }

  @Override
  protected Stream<Entry<K, QueryBuilder>> buildMatchAllValuesQuery(K key, String... values) {
    return Functions.queryToEntry(
        key,
        buildMultiMatchQueryStream(key, values)
            .collect(BoolQueryBuilder::new, BoolQueryBuilder::must, BoolQueryBuilder::must)
            .queryName(ALL_VALUES + key.asCamelCase()));
  }

  private Stream<QueryBuilder> buildMultiMatchQueryStream(K key, String... values) {
    return Arrays.stream(values).map(singleValue -> getMultiMatchQueryBuilder(singleValue, key));
  }

  private QueryBuilder getMultiMatchQueryBuilder(String value, K key) {
    final var searchFields = key.searchFields(EXCLUDE_KEYWORD).toArray(String[]::new);
    return QueryBuilders.multiMatchQuery(value, searchFields)
        .type(MultiMatchQueryBuilder.Type.CROSS_FIELDS)
        .operator(Operator.AND);
  }
}
