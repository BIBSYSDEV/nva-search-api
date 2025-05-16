package no.unit.nva.search.testing.common;

import java.util.Collections;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import no.unit.nva.search.common.QueryFilter;
import no.unit.nva.search.resource.ResourceParameter;
import no.unit.nva.search.resource.ResourceSearchQuery;
import org.mockito.ArgumentMatcher;

public final class ResourceSearchQueryMatcher implements ArgumentMatcher<ResourceSearchQuery> {
  private final Map<ResourceParameter, String> pageParameters;
  private final Map<ResourceParameter, String> searchParameters;
  private final Map<String, TermsQueryBuilderExpectation> namedFilterQueries;

  private ResourceSearchQueryMatcher(
      final Map<ResourceParameter, String> pageParameters,
      final Map<ResourceParameter, String> searchParameters,
      final Map<String, TermsQueryBuilderExpectation> namedFilterQueries) {
    this.pageParameters = Collections.unmodifiableMap(pageParameters);
    this.searchParameters = Collections.unmodifiableMap(searchParameters);
    this.namedFilterQueries = Collections.unmodifiableMap(namedFilterQueries);
  }

  @Override
  public boolean matches(ResourceSearchQuery actual) {
    return hasParameters(actual.parameters().getPageEntries(), pageParameters)
        && hasParameters(actual.parameters().getSearchEntries(), searchParameters)
        && hasNamedTermQuery(actual.filters());
  }

  private boolean hasNamedTermQuery(QueryFilter filters) {
    return namedFilterQueries.entrySet().stream().allMatch(entry -> hasTermsQuery(filters, entry));
  }

  private static boolean hasTermsQuery(
      QueryFilter filters, Entry<String, TermsQueryBuilderExpectation> entry) {
    return filters.hasTermsQuery(
        entry.getKey(), entry.getValue().fieldName(), entry.getValue().values());
  }

  private boolean hasParameters(
      Set<Entry<ResourceParameter, String>> actualParameters,
      Map<ResourceParameter, String> expectedParameters) {
    return expectedParameters.entrySet().stream().allMatch(actualParameters::contains);
  }

  @Override
  public Class<?> type() {
    return ResourceSearchQuery.class;
  }

  public record TermsQueryBuilderExpectation(String fieldName, Object... values) {}

  public static class Builder {
    private final Map<ResourceParameter, String> pageParameters = new ConcurrentHashMap<>();
    private final Map<ResourceParameter, String> searchParameters = new ConcurrentHashMap<>();
    private final Map<String, TermsQueryBuilderExpectation> namedFiltersQueries =
        new ConcurrentHashMap<>();

    public Builder withPageParameter(ResourceParameter pageParameter, String value) {
      this.pageParameters.put(pageParameter, value);
      return this;
    }

    public Builder withSearchParameter(ResourceParameter searchParameter, String value) {
      this.searchParameters.put(searchParameter, value);
      return this;
    }

    public Builder withNamedFilterQuery(
        String name, TermsQueryBuilderExpectation filterExpectation) {
      this.namedFiltersQueries.put(name, filterExpectation);
      return this;
    }

    public ResourceSearchQueryMatcher build() {
      return new ResourceSearchQueryMatcher(pageParameters, searchParameters, namedFiltersQueries);
    }
  }
}
