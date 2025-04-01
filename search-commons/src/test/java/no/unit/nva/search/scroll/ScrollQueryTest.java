package no.unit.nva.search.scroll;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;

import org.junit.jupiter.api.Test;

public class ScrollQueryTest {
  @Test
  void shouldGenerateSensibleQuery() {
    var scrollId = "myScrollId";
    var ttl = "1m";
    var query = ScrollQuery.builder().withTtl(ttl).withScrollId(scrollId).build();

    var scrollClient = mock(ScrollClient.class);

    query.assemble();
    query.doSearch(scrollClient);

    assertEquals(ttl, query.getTtl());
    assertEquals(scrollId, query.getScrollId());
  }
}
