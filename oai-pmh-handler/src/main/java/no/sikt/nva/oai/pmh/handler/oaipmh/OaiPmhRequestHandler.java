package no.sikt.nva.oai.pmh.handler.oaipmh;

import jakarta.xml.bind.JAXBElement;
import no.unit.nva.search.resource.ResourceSort;
import org.openarchives.oai.pmh.v2.OAIPMHtype;

public interface OaiPmhRequestHandler<T> {
  String MODIFIED_DATE_ASCENDING = ResourceSort.MODIFIED_DATE.asCamelCase() + ":asc";

  JAXBElement<OAIPMHtype> handleRequest(T request);
}
