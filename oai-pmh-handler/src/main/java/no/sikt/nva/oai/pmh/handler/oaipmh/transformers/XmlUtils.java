package no.sikt.nva.oai.pmh.handler.oaipmh.transformers;

import static java.util.Objects.isNull;

import org.openarchives.oai.pmh.v2.ElementType;
import org.openarchives.oai.pmh.v2.ObjectFactory;

public final class XmlUtils {

  private static final ObjectFactory OBJECT_FACTORY = new ObjectFactory();

  private XmlUtils() {}

  public static String sanitizeXmlValue(String value) {
    return isNull(value) ? null : value.replaceAll("[\\x00-\\x08\\x0B\\x0C\\x0E-\\x1F]", "").trim();
  }

  public static ElementType createSafeElementType(String value) {
    var element = OBJECT_FACTORY.createElementType();
    element.setValue(sanitizeXmlValue(value));
    return element;
  }
}
