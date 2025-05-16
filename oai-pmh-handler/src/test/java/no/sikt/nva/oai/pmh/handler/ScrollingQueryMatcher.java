package no.sikt.nva.oai.pmh.handler;

import no.unit.nva.search.scroll.ScrollQuery;
import org.mockito.ArgumentMatcher;

public class ScrollingQueryMatcher implements ArgumentMatcher<ScrollQuery> {
  private final String scrollId;
  private final String ttl;

  public ScrollingQueryMatcher(String scrollId, String ttl) {
    this.scrollId = scrollId;
    this.ttl = ttl;
  }

  @Override
  public boolean matches(ScrollQuery actual) {
    return scrollId.equals(actual.getScrollId()) && ttl.equals(actual.getTtl());
  }

  @Override
  public Class<?> type() {
    return ScrollQuery.class;
  }
}
