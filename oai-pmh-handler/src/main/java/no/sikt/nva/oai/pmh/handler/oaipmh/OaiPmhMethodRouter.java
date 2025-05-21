package no.sikt.nva.oai.pmh.handler.oaipmh;

import jakarta.xml.bind.JAXBElement;
import java.net.URI;
import org.openarchives.oai.pmh.v2.OAIPMHtype;

public interface OaiPmhMethodRouter {
  JAXBElement<OAIPMHtype> handleRequest(OaiPmhRequest context, URI endpointUri);
}
