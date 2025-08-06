package no.sikt.nva.oai.pmh.handler.oaipmh.handlers;

import static no.sikt.nva.oai.pmh.handler.JaxbUtils.getSchemaLocation;
import static no.sikt.nva.oai.pmh.handler.oaipmh.OaiPmhUtils.baseResponse;

import jakarta.xml.bind.JAXBElement;
import no.sikt.nva.oai.pmh.handler.JaxbUtils.Namespaces;
import no.sikt.nva.oai.pmh.handler.oaipmh.MetadataPrefix;
import no.sikt.nva.oai.pmh.handler.oaipmh.request.ListMetadataFormatsRequest;
import org.openarchives.oai.pmh.v2.OAIPMHtype;
import org.openarchives.oai.pmh.v2.ObjectFactory;

public class ListMetadataFormatsRequestHandler
    implements OaiPmhRequestHandler<ListMetadataFormatsRequest> {

  public ListMetadataFormatsRequestHandler() {
    // The handler constructor has no arguments.
  }

  @Override
  public JAXBElement<OAIPMHtype> handleRequest(ListMetadataFormatsRequest request) {
    var objectFactory = new ObjectFactory();
    var oaiResponse = baseResponse(objectFactory);
    var value = oaiResponse.getValue();
    value.getRequest().setVerb(request.getVerbType());

    var listMetadataFormatsType = objectFactory.createListMetadataFormatsType();

    var metadataFormatType = objectFactory.createMetadataFormatType();
    metadataFormatType.setMetadataPrefix(MetadataPrefix.OAI_DC.getPrefix());
    metadataFormatType.setSchema(getSchemaLocation(Namespaces.OAI_DC));
    metadataFormatType.setMetadataNamespace(Namespaces.OAI_DC);

    listMetadataFormatsType.getMetadataFormat().add(metadataFormatType);

    value.setListMetadataFormats(listMetadataFormatsType);

    return oaiResponse;
  }
}
