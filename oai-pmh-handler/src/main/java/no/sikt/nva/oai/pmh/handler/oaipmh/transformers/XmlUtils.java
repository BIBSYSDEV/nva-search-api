package no.sikt.nva.oai.pmh.handler.oaipmh.transformers;

import static java.util.Objects.isNull;

import java.util.regex.Pattern;

public final class XmlUtils {

  private static final Pattern XML_ILLEGAL_CONTROL_CHARACTERS =
      Pattern.compile("[\\x00-\\x08\\x0B\\x0C\\x0E-\\x1F]");

  private XmlUtils() {}

  public static String sanitizeXmlValue(String value) {
    return isNull(value)
        ? null
        : XML_ILLEGAL_CONTROL_CHARACTERS.matcher(value).replaceAll("").trim();
  }
}
