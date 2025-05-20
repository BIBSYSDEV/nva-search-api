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
  private static final String UNKNOWN_OR_NO_VERB_SUPPLIED = "Unknown or no verb supplied.";

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
    return request
        .getVerb()
        .map(
            type ->
                switch (type) {
                  case LIST_SETS -> new ListSets().listSets(resourceClient);
                  case IDENTIFY -> new Identify().identify(endpointUri);
                  case LIST_METADATA_FORMATS -> new ListMetadataFormats().listMetadataFormats();
                  case LIST_RECORDS ->
                      new ListRecords(resourceClient, recordTransformer, batchSize)
                          .listRecords(request);
                  default -> badVerb(UNSUPPORTED_VERB);
                })
        .orElseGet(() -> badVerb(UNKNOWN_OR_NO_VERB_SUPPLIED));
  }

  private JAXBElement<OAIPMHtype> badVerb(String message) {
    var objectFactory = new ObjectFactory();
    var response = baseResponse(objectFactory);
    var value = response.getValue();
    var error = objectFactory.createOAIPMHerrorType();
    error.setCode(OAIPMHerrorcodeType.BAD_VERB);
    error.setValue(message);

    value.getError().add(error);

    return response;
  }
}
