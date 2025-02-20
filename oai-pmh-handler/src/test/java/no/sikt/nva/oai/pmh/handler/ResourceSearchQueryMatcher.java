package no.sikt.nva.oai.pmh.handler;

import java.util.Objects;
import no.unit.nva.search.resource.ResourceParameter;
import no.unit.nva.search.resource.ResourceSearchQuery;
import org.mockito.ArgumentMatcher;

public class ResourceSearchQueryMatcher implements ArgumentMatcher<ResourceSearchQuery> {
  private final Integer from;
  private final int size;
  private final String aggregation;

  public ResourceSearchQueryMatcher(Integer from, int size, String aggregation) {
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
    var actualFrom = actual.parameters().get(ResourceParameter.FROM);
    return Objects.isNull(from) ? actualFrom.isEmpty() : from.equals(actualFrom.asNumber());
  }

  @Override
  public Class<?> type() {
    return ResourceSearchQuery.class;
  }
}
