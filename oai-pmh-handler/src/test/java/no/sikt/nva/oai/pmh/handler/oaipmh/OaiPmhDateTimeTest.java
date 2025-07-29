package no.sikt.nva.oai.pmh.handler.oaipmh;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

public class OaiPmhDateTimeTest {

  @Test
  void shouldThrowExceptionWhenDateTimeIsInvalid() {
    assertThrows(BadArgumentException.class, () -> OaiPmhDateTime.from("2021-12-31T27:61:61Z"));
  }

  @Test
  void shouldThrowExceptionWhenDateIsInvalid() {
    assertThrows(BadArgumentException.class, () -> OaiPmhDateTime.from("2021-13-32"));
  }

  @Test
  void shouldHandleDateOnly() {
    var dateTime = OaiPmhDateTime.from("2020-01-01");

    assertThat(dateTime.asString(), is(equalTo("2020-01-01T00:00:00Z")));
  }

  @Test
  void shouldHandleDateTime() {
    var dateTime = OaiPmhDateTime.from("2020-01-01T01:02:03Z");

    assertThat(dateTime.asString(), is(equalTo("2020-01-01T01:02:03Z")));
  }
}
