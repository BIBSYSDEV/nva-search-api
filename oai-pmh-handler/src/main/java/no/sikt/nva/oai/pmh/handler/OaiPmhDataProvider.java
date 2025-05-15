package no.sikt.nva.oai.pmh.handler;

import jakarta.xml.bind.JAXBElement;
import java.net.URI;
import org.openarchives.oai.pmh.v2.OAIPMHtype;

public interface OaiPmhDataProvider {
  String OAI_DC_METADATA_PREFIX = "oai-dc";

  JAXBElement<OAIPMHtype> handleRequest(
      String verb,
      String from,
      String until,
      String metadataPrefix,
      String resumptionToken,
      URI endpointUri);
}
