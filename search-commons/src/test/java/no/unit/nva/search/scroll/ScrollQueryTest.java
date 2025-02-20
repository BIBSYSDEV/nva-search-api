package no.unit.nva.search.scroll;

import static org.mockito.Mockito.mock;

import org.junit.jupiter.api.Test;

public class ScrollQueryTest {
  @Test
  void shouldGenerateSensibleQuery() {
    var scrollId = "myScrollId";
    var query = ScrollQuery.builder().withTtl("1m").withScrollId(scrollId).build();

    var scrollClient = mock(ScrollClient.class);

    query.doSearch(scrollClient);
  }
}
