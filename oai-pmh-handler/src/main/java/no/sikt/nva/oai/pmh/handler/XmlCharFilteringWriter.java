package no.sikt.nva.oai.pmh.handler;

import java.io.FilterWriter;
import java.io.IOException;
import java.io.Writer;

final class XmlCharFilteringWriter extends FilterWriter {

  private static final int TAB = 0x9;
  private static final int LINE_FEED = 0xA;
  private static final int CARRIAGE_RETURN = 0xD;
  private static final int FIRST_PRINTABLE = 0x20;
  private static final int LAST_VALID_BMP = 0xFFFD;

  XmlCharFilteringWriter(Writer delegate) {
    super(delegate);
  }

  @Override
  public void write(int character) throws IOException {
    if (isValidXmlChar(character)) {
      super.write(character);
    }
  }

  @Override
  public void write(char[] buffer, int offset, int length) throws IOException {
    var sanitized = new char[length];
    var index = 0;
    for (var position = 0; position < length; position++) {
      var character = buffer[offset + position];
      if (isValidXmlChar(character)) {
        sanitized[index++] = character;
      }
    }
    super.write(sanitized, 0, index);
  }

  @Override
  public void write(String value, int offset, int length) throws IOException {
    write(value.toCharArray(), offset, length);
  }

  private static boolean isValidXmlChar(int character) {
    return character == TAB
        || character == LINE_FEED
        || character == CARRIAGE_RETURN
        || (character >= FIRST_PRINTABLE && character <= LAST_VALID_BMP);
  }
}
