package no.sikt.nva.oai.pmh.handler.oaipmh;

import static no.sikt.nva.oai.pmh.handler.oaipmh.OaiPmhUtils.baseResponse;

import jakarta.xml.bind.JAXBElement;
import java.net.URI;
import no.unit.nva.search.resource.ResourceClient;
import org.openarchives.oai.pmh.v2.OAIPMHerrorcodeType;
import org.openarchives.oai.pmh.v2.OAIPMHtype;
import org.openarchives.oai.pmh.v2.ObjectFactory;

public class DefaultOaiPmhMethodRouter implements OaiPmhMethodRouter {

  private static final String UNSUPPORTED_VERB = "Unsupported verb.";

  private final ResourceClient resourceClient;
  private final RecordTransformer recordTransformer;
  private final int batchSize;

  public DefaultOaiPmhMethodRouter(ResourceClient resourceClient, int batchSize) {
    this.resourceClient = resourceClient;
    this.recordTransformer = new SimplifiedRecordTransformer();
    this.batchSize = batchSize;
  }

  @Override
  public JAXBElement<OAIPMHtype> handleRequest(final OaiPmhRequest request, final URI endpointUri) {
    try {
      return switch (request.getVerbType()) {
        case LIST_SETS -> new ListSets().listSets((ListSetsRequest) request, resourceClient);
        case IDENTIFY -> new Identify().identify((IdentifyRequest) request, endpointUri);
        case LIST_METADATA_FORMATS ->
            new ListMetadataFormats().listMetadataFormats((ListMetadataFormatsRequest) request);
        case LIST_RECORDS ->
            new ListRecords(resourceClient, recordTransformer, batchSize)
                .listRecords((ListRecordsRequest) request);
        default -> error(OAIPMHerrorcodeType.BAD_VERB, UNSUPPORTED_VERB);
      };
    } catch (OaiPmhException exception) {
      return error(exception.getCodeType(), exception.getMessage());
    }
  }

  public static JAXBElement<OAIPMHtype> error(OAIPMHerrorcodeType codeType, String message) {
    var objectFactory = new ObjectFactory();
    var response = baseResponse(objectFactory);
    var value = response.getValue();
    var error = objectFactory.createOAIPMHerrorType();
    error.setCode(codeType);
    error.setValue(message);

    value.getError().add(error);

    return response;
  }
}
