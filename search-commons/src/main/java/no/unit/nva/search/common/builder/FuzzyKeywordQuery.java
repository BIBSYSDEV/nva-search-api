package no.unit.nva.search.common.builder;

import static no.unit.nva.constants.Words.KEYWORD_FALSE;
import static no.unit.nva.constants.Words.KEYWORD_TRUE;

import java.util.Arrays;
import java.util.Map.Entry;
import java.util.stream.Stream;
import no.unit.nva.search.common.constant.Functions;
import no.unit.nva.search.common.enums.ParameterKey;
import org.opensearch.common.unit.Fuzziness;
import org.opensearch.index.query.DisMaxQueryBuilder;
import org.opensearch.index.query.Operator;
import org.opensearch.index.query.QueryBuilder;
import org.opensearch.index.query.QueryBuilders;
import org.opensearch.index.query.TermQueryBuilder;
import org.opensearch.index.query.TermsQueryBuilder;

/**
 * Class for building OpenSearch queries that search for keywords with fuzzy matching.
 *
 * @author Stig Norland
 * @param <K> the type of the parameter keys used in the query. The parameter keys are used to
 *     define the parameters that can be used in the query.
 */
public class FuzzyKeywordQuery<K extends Enum<K> & ParameterKey<K>> extends AbstractBuilder<K> {

  public static final String KEYWORD_ANY = "FuzzyKeywordAny-";
  public static final String KEYWORD_ALL = "FuzzyKeywordAll-";

  @Override
  protected Stream<Entry<K, QueryBuilder>> buildMatchAnyValueQuery(K key, String... values) {
    var boolQuery =
        QueryBuilders.boolQuery()
            .should(buildMatchAnyKeyword(key, values))
            .must(buildMatchAnyFuzzy(key, values))
            .queryName(KEYWORD_ANY + key.asCamelCase());
    return Functions.queryToEntry(key, boolQuery);
  }

  @Override
  protected Stream<Entry<K, QueryBuilder>> buildMatchAllValuesQuery(K key, String... values) {
    var boolQuery = QueryBuilders.boolQuery().queryName(KEYWORD_ALL + key.asCamelCase());
    buildMatchAllKeyword(key, values).forEach(boolQuery::should);
    buildMatchAllFuzzy(key, values).forEach(boolQuery::must);

    return Functions.queryToEntry(key, boolQuery);
  }

  private DisMaxQueryBuilder buildMatchAnyKeyword(K key, String... values) {
    return key.searchFields(KEYWORD_TRUE)
        .map(searchField -> new TermsQueryBuilder(searchField, values))
        .collect(DisMaxQueryBuilder::new, DisMaxQueryBuilder::add, DisMaxQueryBuilder::add)
        .boost(key.fieldBoost());
  }

  private DisMaxQueryBuilder buildMatchAnyFuzzy(K key, String... values) {
    return Arrays.stream(values)
        .map(value -> getMultiMatchQueryBuilder(value, key))
        .collect(DisMaxQueryBuilder::new, DisMaxQueryBuilder::add, DisMaxQueryBuilder::add);
  }

  private QueryBuilder getMultiMatchQueryBuilder(String value, K key) {
    final var searchFields = key.searchFields(KEYWORD_FALSE).toArray(String[]::new);
    return QueryBuilders.multiMatchQuery(value, searchFields)
        .fuzziness(Fuzziness.ZERO)
        .maxExpansions(10)
        .operator(Operator.AND);
  }

  private Stream<DisMaxQueryBuilder> buildMatchAllKeyword(K key, String... values) {
    return Arrays.stream(values)
        .map(
            value ->
                key.searchFields(KEYWORD_TRUE)
                    .map(searchField -> new TermQueryBuilder(searchField, value))
                    .collect(
                        DisMaxQueryBuilder::new, DisMaxQueryBuilder::add, DisMaxQueryBuilder::add)
                    .boost(key.fieldBoost()));
  }

  private Stream<DisMaxQueryBuilder> buildMatchAllFuzzy(K key, String... values) {
    return Arrays.stream(values)
        .map(
            singleValue ->
                getMatchPhrasePrefixBuilderStream(singleValue, key)
                    .collect(
                        DisMaxQueryBuilder::new, DisMaxQueryBuilder::add, DisMaxQueryBuilder::add));
  }

  private Stream<QueryBuilder> getMatchPhrasePrefixBuilderStream(String singleValue, K key) {
    return key.searchFields(KEYWORD_FALSE)
        .map(
            fieldName ->
                QueryBuilders.matchPhrasePrefixQuery(fieldName, singleValue).maxExpansions(10));
  }
}
