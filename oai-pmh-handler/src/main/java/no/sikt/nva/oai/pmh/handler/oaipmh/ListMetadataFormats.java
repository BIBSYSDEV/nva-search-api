package no.sikt.nva.oai.pmh.handler.oaipmh;

import static no.sikt.nva.oai.pmh.handler.JaxbUtils.getSchemaLocation;
import static no.sikt.nva.oai.pmh.handler.oaipmh.OaiPmhUtils.baseResponse;

import jakarta.xml.bind.JAXBElement;
import no.sikt.nva.oai.pmh.handler.JaxbUtils.Namespaces;
import org.openarchives.oai.pmh.v2.OAIPMHtype;
import org.openarchives.oai.pmh.v2.ObjectFactory;
import org.openarchives.oai.pmh.v2.VerbType;

public class ListMetadataFormats {
  private static final String OAI_DC_METADATA_PREFIX = "oai-dc";

  public ListMetadataFormats() {}

  public JAXBElement<OAIPMHtype> listMetadataFormats() {
    var objectFactory = new ObjectFactory();
    var oaiResponse = baseResponse(objectFactory);
    var value = oaiResponse.getValue();
    value.getRequest().setVerb(VerbType.LIST_METADATA_FORMATS);

    var listMetadataFormatsType = objectFactory.createListMetadataFormatsType();

    var metadataFormatType = objectFactory.createMetadataFormatType();
    metadataFormatType.setMetadataPrefix(OAI_DC_METADATA_PREFIX);
    metadataFormatType.setSchema(getSchemaLocation(Namespaces.OAI_DC));
    metadataFormatType.setMetadataNamespace(Namespaces.OAI_DC);

    listMetadataFormatsType.getMetadataFormat().add(metadataFormatType);

    value.setListMetadataFormats(listMetadataFormatsType);

    return oaiResponse;
  }
}
