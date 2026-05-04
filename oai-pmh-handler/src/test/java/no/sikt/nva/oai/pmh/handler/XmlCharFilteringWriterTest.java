package no.sikt.nva.oai.pmh.handler;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.core.Is.is;

import java.io.IOException;
import java.io.StringWriter;
import org.junit.jupiter.api.Test;

class XmlCharFilteringWriterTest {

  @Test
  void shouldDropIllegalControlCharacterWhenWritingSingleChar() throws IOException {
    var sink = new StringWriter();
    try (var writer = new XmlCharFilteringWriter(sink)) {
      writer.write('a');
      writer.write(0x01);
      writer.write('b');
    }
    assertThat(sink.toString(), is(equalTo("ab")));
  }

  @Test
  void shouldDropIllegalControlCharactersWhenWritingCharArraySlice() throws IOException {
    var sink = new StringWriter();
    var buffer = ("xNat" + (char) 0x01 + "alie" + (char) 0x0B + "y").toCharArray();
    try (var writer = new XmlCharFilteringWriter(sink)) {
      writer.write(buffer, 1, buffer.length - 2);
    }
    assertThat(sink.toString(), is(equalTo("Natalie")));
  }

  @Test
  void shouldDropIllegalControlCharactersWhenWritingStringSlice() throws IOException {
    var sink = new StringWriter();
    var input = "ignoreP" + (char) 0x01 + "er Nor" + (char) 0x01 + "dmannignore";
    try (var writer = new XmlCharFilteringWriter(sink)) {
      writer.write(input, "ignore".length(), input.length() - 2 * "ignore".length());
    }
    assertThat(sink.toString(), is(equalTo("Per Nordmann")));
  }

  @Test
  void shouldPreserveValidWhitespaceAndPrintableCharacters() throws IOException {
    var sink = new StringWriter();
    var input = "tab\there\nline\rcr ascii";
    try (var writer = new XmlCharFilteringWriter(sink)) {
      writer.write(input, 0, input.length());
    }
    assertThat(sink.toString(), is(equalTo(input)));
  }
}
