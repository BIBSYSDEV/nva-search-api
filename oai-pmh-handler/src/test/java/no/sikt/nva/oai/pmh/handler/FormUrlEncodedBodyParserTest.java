package no.sikt.nva.oai.pmh.handler;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.core.Is.is;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
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

  @ParameterizedTest(name = "should URL decode query value {0} to {1}")
  @MethodSource("urlDecodeInputAndOutput")
  void shouldUrlDecodeValues(String input, String expected) {
    var value = FormUrlencodedBodyParser.from(input).getValue("query");
    assertThat(value.orElseThrow(), is(equalTo((expected))));
  }

  private static Stream<Arguments> urlDecodeInputAndOutput() {
    return Stream.of(
        Arguments.of("query=Ola%20Nordmann", "Ola Nordmann"), // space as %20
        Arguments.of("query=Ola+Nordmann", "Ola Nordmann"), // space as plus
        Arguments.of("query=hello%26world%3Dtest", "hello&world=test"), // special characters
        Arguments.of("query=%40%23%24%25%5E%26%2A%28%29", "@#$%^&*()"), // symbols
        Arguments.of("query=%E4%BD%A0%E5%A5%BD", "你好")); // non-ascii
  }
}
