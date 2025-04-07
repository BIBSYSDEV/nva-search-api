package no.unit.nva.search.common;

import static java.util.Objects.nonNull;
import static no.unit.nva.constants.Words.COMMA;

import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.opensearch.index.query.BoolQueryBuilder;
import org.opensearch.index.query.QueryBuilder;
import org.opensearch.index.query.QueryBuilders;
import org.opensearch.index.query.TermsQueryBuilder;

/**
 * QueryFilter is a class that represents a query filter to the search service.
 *
 * @author Stig Norland
 */
public class QueryFilter {
  private static final String SUFFIX = "]";
  private static final String PREFIX = "[";

  private final transient Map<String, QueryBuilder> filters = new HashMap<>();

  public QueryFilter() {}

  public BoolQueryBuilder get() {
    var boolQueryBuilder = QueryBuilders.boolQuery();
    filters.values().forEach(boolQueryBuilder::must);
    return boolQueryBuilder;
  }

  /**
   * Clears filter and sets new filters.
   *
   * @param filters QueryBuilder
   */
  public void set(QueryBuilder... filters) {
    this.filters.clear();
    Arrays.stream(filters).forEach(this::add);
  }

  public int size() {
    return filters.size();
  }

  public boolean hasTermsQuery(String queryName, String fieldName, Object... values) {
    var query = this.filters.get(queryName);
    if (nonNull(query) && query instanceof TermsQueryBuilder termsQueryBuilder) {
      return termsQueryBuilder.fieldName().equals(fieldName)
          && new HashSet<>(termsQueryBuilder.values()).containsAll(List.of(values));
    }
    return false;
  }

  public QueryFilter add(QueryBuilder builder) {
    this.filters.put(builder.queryName(), builder);
    return this;
  }

  @Override
  public String toString() {
    return filters.values().stream()
        .map(QueryBuilder::toString)
        .collect(Collectors.joining(COMMA, PREFIX, SUFFIX));
  }
}
