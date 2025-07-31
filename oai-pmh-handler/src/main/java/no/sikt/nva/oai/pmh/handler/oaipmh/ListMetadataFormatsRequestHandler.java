package no.sikt.nva.oai.pmh.handler.oaipmh;

import static no.sikt.nva.oai.pmh.handler.JaxbUtils.getSchemaLocation;
import static no.sikt.nva.oai.pmh.handler.oaipmh.OaiPmhUtils.baseResponse;

import jakarta.xml.bind.JAXBElement;
import no.sikt.nva.oai.pmh.handler.JaxbUtils.Namespaces;
import org.openarchives.oai.pmh.v2.OAIPMHtype;
import org.openarchives.oai.pmh.v2.ObjectFactory;

public class ListMetadataFormatsRequestHandler {

  public ListMetadataFormatsRequestHandler() {}

  public JAXBElement<OAIPMHtype> listMetadataFormats(ListMetadataFormatsRequest request) {
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
