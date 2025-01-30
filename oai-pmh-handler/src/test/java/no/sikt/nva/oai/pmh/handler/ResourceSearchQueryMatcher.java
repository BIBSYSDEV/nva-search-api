package no.sikt.nva.oai.pmh.handler;

import no.unit.nva.search.resource.ResourceParameter;
import no.unit.nva.search.resource.ResourceSearchQuery;
import org.mockito.ArgumentMatcher;

public class ResourceSearchQueryMatcher implements ArgumentMatcher<ResourceSearchQuery> {
  private final int from;
  private final int size;
  private final String aggregation;

  public ResourceSearchQueryMatcher(int from, int size, String aggregation) {
    this.from = from;
    this.size = size;
    this.aggregation = aggregation;
  }

  @Override
  public boolean matches(ResourceSearchQuery actual) {
    return hasSameOffset(actual) && hasSameSize(actual) && hasSameAggregations(actual);
  }

  private boolean hasSameAggregations(ResourceSearchQuery actual) {
    return aggregation.equals(actual.parameters().get(ResourceParameter.AGGREGATION).toString());
  }

  private boolean hasSameSize(ResourceSearchQuery actual) {
    return size == actual.parameters().get(ResourceParameter.SIZE).asNumber().intValue();
  }

  private boolean hasSameOffset(ResourceSearchQuery actual) {
    return from == actual.parameters().get(ResourceParameter.FROM).asNumber().intValue();
  }

  @Override
  public Class<?> type() {
    return ResourceSearchQuery.class;
  }
}
