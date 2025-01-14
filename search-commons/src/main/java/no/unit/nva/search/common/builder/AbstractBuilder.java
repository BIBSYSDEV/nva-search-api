package no.unit.nva.search.common.builder;

import static no.unit.nva.constants.Words.COMMA;
import static no.unit.nva.search.common.enums.FieldOperator.ANY_OF;
import static no.unit.nva.search.common.enums.FieldOperator.BETWEEN;
import static no.unit.nva.search.common.enums.FieldOperator.NOT_ANY_OF;

import java.util.Map;
import java.util.Map.Entry;
import java.util.stream.Stream;
import no.unit.nva.search.common.enums.ParameterKey;
import nva.commons.core.JacocoGenerated;
import org.opensearch.index.query.QueryBuilder;
import org.opensearch.index.query.QueryBuilders;

/**
 * Abstract class for building OpenSearch queries.
 *
 * <ul>
 *   <li>The query can be built as a single query or as a multi query.
 *   <li>One or more values can be added to a query.
 *   <li>One or mode fields can be added to a query.
 *   <li>SHOULD, MUST_NOT implement OR operator
 *   <li>MUST implement AND operator
 * </ul>
 *
 * @author Stig Norland
 * @param <K> the type of the parameter keys used in the query. The parameter keys are used to
 *     define the parameters that can be used in the query.
 */
public abstract class AbstractBuilder<K extends Enum<K> & ParameterKey<K>> {

  @JacocoGenerated
  abstract Stream<Entry<K, QueryBuilder>> buildMatchAnyValueQuery(K key, String... values);

  @JacocoGenerated
  abstract Stream<Entry<K, QueryBuilder>> buildMatchAllValuesQuery(K key, String... values);

  public Stream<Map.Entry<K, QueryBuilder>> buildQuery(K key, String value) {
    final var values = splitAndFixMissingRangeValue(key, value);
    return buildQuery(key, values);
  }

  public Stream<Map.Entry<K, QueryBuilder>> buildQuery(K key, String... values) {
    return isSearchAny(key)
        ? buildMatchAnyValueQuery(key, values)
        : buildMatchAllValuesQuery(key, values);
  }

  protected QueryBuilder getSubQuery(K key, String... values) {
    return switch (key.fieldType()) {
      case KEYWORD ->
          new KeywordQuery<K>().buildQuery(key, values).findFirst().orElseThrow().getValue();
      case FUZZY_KEYWORD ->
          new FuzzyKeywordQuery<K>().buildQuery(key, values).findFirst().orElseThrow().getValue();
      case TEXT -> new TextQuery<K>().buildQuery(key, values).findFirst().orElseThrow().getValue();
      case ACROSS_FIELDS ->
          new AcrossFieldsQuery<K>().buildQuery(key, values).findFirst().orElseThrow().getValue();
      case FREE_TEXT -> QueryBuilders.matchAllQuery();
      default -> throw new IllegalStateException("Unexpected value: " + key.fieldType());
    };
  }

  private boolean isSearchAny(K key) {
    return key.searchOperator().equals(ANY_OF) || key.searchOperator().equals(NOT_ANY_OF);
  }

  private boolean isRangeMissingComma(K key, String value) {
    return key.searchOperator().equals(BETWEEN) && !value.contains(COMMA);
  }

  private String[] splitAndFixMissingRangeValue(K key, String value) {
    return isRangeMissingComma(key, value) ? new String[] {value, value} : value.split(COMMA);
  }
}
