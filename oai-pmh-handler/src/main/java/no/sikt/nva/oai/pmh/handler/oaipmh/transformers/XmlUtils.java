package no.sikt.nva.oai.pmh.handler.oaipmh.transformers;

import static java.util.Objects.isNull;

public final class XmlUtils {

  private XmlUtils() {}

  public static String sanitizeXmlValue(String value) {
    return isNull(value) ? null : value.replaceAll("[\\x00-\\x08\\x0B\\x0C\\x0E-\\x1F]", "").trim();
  }
}
