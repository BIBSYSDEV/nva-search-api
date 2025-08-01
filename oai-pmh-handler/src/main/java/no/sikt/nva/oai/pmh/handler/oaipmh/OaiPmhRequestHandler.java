package no.sikt.nva.oai.pmh.handler.oaipmh;

import jakarta.xml.bind.JAXBElement;
import org.openarchives.oai.pmh.v2.OAIPMHtype;

public interface OaiPmhRequestHandler<T> {
  JAXBElement<OAIPMHtype> handleRequest(T request);
}
