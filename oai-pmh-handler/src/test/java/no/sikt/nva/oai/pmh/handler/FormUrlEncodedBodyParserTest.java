package no.sikt.nva.oai.pmh.handler;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.core.Is.is;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

public class FormUrlEncodedBodyParserTest {
  @Test
  void shouldThrowIllegalArgumentExceptionWhenSourceIsNull() {
    assertThrows(IllegalArgumentException.class, () -> FormUrlencodedBodyParser.from(null));
  }

  @ParameterizedTest(name = "illegal argument when source is {0}")
  @ValueSource(strings = {"", " ", "abc", "key=&="})
  void shouldThrowIllegalArgumentExceptionWhenSourceDoesNoComply(String source) {
    assertThrows(IllegalArgumentException.class, () -> FormUrlencodedBodyParser.from(source));
  }

  @ParameterizedTest(name = "should URL decode {0}")
  @ValueSource(strings = {"name=Ola%20Nordmann", "name=Ola+Nordmann"})
  void shouldUrlDecodeValues(String source) {
    var value = FormUrlencodedBodyParser.from(source).getValue("name");
    assertThat(value.orElseThrow(), is(equalTo(("Ola Nordmann"))));
  }
}
