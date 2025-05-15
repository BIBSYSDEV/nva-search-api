package no.sikt.nva.oai.pmh.handler;

import jakarta.xml.bind.Marshaller;
import jakarta.xml.bind.PropertyException;
import java.util.LinkedHashMap;
import java.util.Map;
import org.glassfish.jaxb.runtime.marshaller.NamespacePrefixMapper;

public final class JaxbUtils {

  private static final Map<String, String> NAMESPACE_PREFIXES =
      Map.of(
          Namespaces.OAI_PMH, "", // default
          Namespaces.XSI, "xsi",
          Namespaces.DC, "dc",
          Namespaces.OAI_DC, "oai_dc");

  private static final Map<String, String> SCHEMA_LOCATIONS = new LinkedHashMap<>();

  static {
    SCHEMA_LOCATIONS.put(Namespaces.OAI_PMH, Namespaces.OAI_PMH_XSD);
    SCHEMA_LOCATIONS.put(Namespaces.DC, Namespaces.DC_XSD);
    SCHEMA_LOCATIONS.put(Namespaces.OAI_DC, Namespaces.OAI_DC_XSD);
  }

  private JaxbUtils() {}

  public static String getSchemaLocation(String namespace) {
    return SCHEMA_LOCATIONS.get(namespace);
  }

  public static void configureMarshaller(Marshaller marshaller) throws PropertyException {
    marshaller.setProperty(
        "org.glassfish.jaxb.namespacePrefixMapper", new CustomNamespacePrefixMapper());
    marshaller.setProperty(Marshaller.JAXB_SCHEMA_LOCATION, buildSchemaLocationString());
  }

  private static String buildSchemaLocationString() {
    return SCHEMA_LOCATIONS.entrySet().stream()
        .map(entry -> entry.getKey() + " " + entry.getValue())
        .reduce((key, value) -> key + " " + value)
        .orElse("");
  }

  public static class CustomNamespacePrefixMapper extends NamespacePrefixMapper {
    @Override
    public String getPreferredPrefix(
        String namespaceUri, String suggestion, boolean requirePrefix) {
      return NAMESPACE_PREFIXES.getOrDefault(namespaceUri, suggestion);
    }
  }

  public static final class Namespaces {
    public static final String OAI_PMH = "http://www.openarchives.org/OAI/2.0/";
    public static final String XSI = "http://www.w3.org/2001/XMLSchema-instance";
    public static final String DC = "http://purl.org/dc/elements/1.1/";
    public static final String OAI_DC = "http://www.openarchives.org/OAI/2.0/oai_dc/";

    public static final String OAI_PMH_XSD = "http://www.openarchives.org/OAI/2.0/OAI-PMH.xsd";
    public static final String DC_XSD = "http://dublincore.org/schemas/xmls/simpledc20021212.xsd";
    public static final String OAI_DC_XSD = "http://www.openarchives.org/OAI/2.0/oai_dc.xsd";

    private Namespaces() {
      // prevent instantiation
    }
  }
}
