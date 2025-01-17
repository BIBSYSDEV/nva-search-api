package no.unit.nva.search.common.builder;

import static no.unit.nva.constants.Words.EXCLUDE_KEYWORD;
import static org.opensearch.index.query.QueryBuilders.matchPhrasePrefixQuery;
import static org.opensearch.index.query.QueryBuilders.matchQuery;

import java.util.Arrays;
import java.util.Map.Entry;
import java.util.stream.Stream;
import no.unit.nva.search.common.constant.Functions;
import no.unit.nva.search.common.enums.ParameterKey;
import org.opensearch.index.query.DisMaxQueryBuilder;
import org.opensearch.index.query.MatchPhrasePrefixQueryBuilder;
import org.opensearch.index.query.MatchQueryBuilder;
import org.opensearch.index.query.Operator;
import org.opensearch.index.query.QueryBuilder;
import org.opensearch.index.query.QueryBuilders;

/**
 * Class for building OpenSearch queries that search for text.
 *
 * @author Stig Norland
 * @param <K> the type of the parameter keys used in the query. The parameter keys are used to
 *     define the parameters that can be used in the query.
 */
public class TextQuery<K extends Enum<K> & ParameterKey<K>> extends AbstractBuilder<K> {

  private static final String TEXT_ALL = "TextAll-";
  private static final String TEXT_ANY = "TextAny-";

  @Override
  protected Stream<Entry<K, QueryBuilder>> buildMatchAnyValueQuery(K key, String... values) {
    return buildAnyComboMustHitQuery(key, values)
        .flatMap(builder -> Functions.queryToEntry(key, builder));
  }

  @Override
  protected Stream<Entry<K, QueryBuilder>> buildMatchAllValuesQuery(K key, String... values) {
    return buildAllMustHitQuery(key, values)
        .flatMap(builder -> Functions.queryToEntry(key, builder));
  }

  private Stream<QueryBuilder> buildAllMustHitQuery(K key, String... values) {
    return Arrays.stream(values)
        .map(
            singleValue ->
                phrasePrefixBuilder(singleValue, key)
                    .collect(
                        DisMaxQueryBuilder::new, DisMaxQueryBuilder::add, DisMaxQueryBuilder::add)
                    .queryName(TEXT_ALL + key.asCamelCase()));
  }

  private Stream<DisMaxQueryBuilder> buildAnyComboMustHitQuery(K key, String... values) {
    var disMax = QueryBuilders.disMaxQuery().queryName(TEXT_ANY + key.asCamelCase());
    Arrays.stream(values)
        .flatMap(singleValue -> phrasePrefixBuilder(singleValue, key))
        .forEach(disMax::add);
    return Stream.of(disMax);
  }

  private Stream<QueryBuilder> phrasePrefixBuilder(String singleValue, K key) {
    return Stream.concat(
        key.searchFields(EXCLUDE_KEYWORD)
            .map(fieldName -> matchPhrasePrefixBuilder(singleValue, key, fieldName)),
        key.searchFields(EXCLUDE_KEYWORD)
            .map(fieldName -> matchQueryBuilder(singleValue, key, fieldName)));
  }

  private MatchQueryBuilder matchQueryBuilder(String singleValue, K key, String fieldName) {
    return matchQuery(fieldName, singleValue).operator(Operator.AND).boost(key.fieldBoost());
  }

  private MatchPhrasePrefixQueryBuilder matchPhrasePrefixBuilder(
      String singleValue, K key, String fieldName) {
    return matchPhrasePrefixQuery(fieldName, singleValue).boost(key.fieldBoost() + 0.1F);
  }
}
