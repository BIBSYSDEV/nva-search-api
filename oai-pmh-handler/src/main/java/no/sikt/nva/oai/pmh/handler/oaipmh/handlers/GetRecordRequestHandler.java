package no.sikt.nva.oai.pmh.handler.oaipmh.handlers;

import static no.sikt.nva.oai.pmh.handler.oaipmh.OaiPmhUtils.baseResponse;

import jakarta.xml.bind.JAXBElement;
import no.sikt.nva.oai.pmh.handler.oaipmh.IdDoesNotExistException;
import no.sikt.nva.oai.pmh.handler.oaipmh.RecordTransformer;
import no.sikt.nva.oai.pmh.handler.oaipmh.request.GetRecordRequest;
import no.sikt.nva.oai.pmh.handler.repository.ResourceRepository;
import org.openarchives.oai.pmh.v2.GetRecordType;
import org.openarchives.oai.pmh.v2.OAIPMHtype;
import org.openarchives.oai.pmh.v2.ObjectFactory;
import org.openarchives.oai.pmh.v2.RecordType;
import org.openarchives.oai.pmh.v2.VerbType;

public class GetRecordRequestHandler implements OaiPmhRequestHandler<GetRecordRequest> {
  private final ResourceRepository resourceRepository;
  private final RecordTransformer recordTransformer;

  public GetRecordRequestHandler(
      ResourceRepository resourceRepository, RecordTransformer recordTransformer) {
    this.resourceRepository = resourceRepository;
    this.recordTransformer = recordTransformer;
  }

  @Override
  public JAXBElement<OAIPMHtype> handleRequest(GetRecordRequest request) {
    var objectFactory = new ObjectFactory();

    var oaiResponse = createBaseResponse(request, objectFactory);

    var resource =
        resourceRepository
            .fetchByIdentifier(request.getIdentifier())
            .orElseThrow(IdDoesNotExistException::new);

    var record = recordTransformer.transform(resource);
    var getRecord = createGetRecordResponse(record, objectFactory);

    oaiResponse.getValue().setGetRecord(getRecord);
    return oaiResponse;
  }

  private GetRecordType createGetRecordResponse(RecordType record, ObjectFactory objectFactory) {
    var getRecord = objectFactory.createGetRecordType();
    getRecord.setRecord(record);
    return getRecord;
  }

  private JAXBElement<OAIPMHtype> createBaseResponse(
      GetRecordRequest request, ObjectFactory objectFactory) {
    var oaiResponse = baseResponse(objectFactory);
    populateGetRecordRequest(request, oaiResponse.getValue());
    return oaiResponse;
  }

  private static void populateGetRecordRequest(GetRecordRequest request, OAIPMHtype oaiPmhType) {
    oaiPmhType.getRequest().setVerb(VerbType.GET_RECORD);
    oaiPmhType.getRequest().setMetadataPrefix(request.getMetadataPrefix().getPrefix());
    oaiPmhType.getRequest().setIdentifier(request.getIdentifier());
  }
}
