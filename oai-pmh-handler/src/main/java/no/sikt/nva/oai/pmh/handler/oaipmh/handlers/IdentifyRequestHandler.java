package no.sikt.nva.oai.pmh.handler.oaipmh.handlers;

import static java.util.Objects.nonNull;
import static no.sikt.nva.oai.pmh.handler.oaipmh.OaiPmhUtils.baseResponse;

import jakarta.xml.bind.JAXBElement;
import java.net.URI;
import java.time.Instant;
import no.sikt.nva.oai.pmh.handler.oaipmh.OaiPmhDateTime;
import no.sikt.nva.oai.pmh.handler.oaipmh.OaiPmhDateTimeUtils;
import no.sikt.nva.oai.pmh.handler.oaipmh.SetSpec;
import no.sikt.nva.oai.pmh.handler.oaipmh.request.IdentifyRequest;
import no.sikt.nva.oai.pmh.handler.repository.ResourceRepository;
import no.unit.nva.search.resource.response.ResourceSearchResponse;
import org.openarchives.oai.pmh.v2.DeletedRecordType;
import org.openarchives.oai.pmh.v2.GranularityType;
import org.openarchives.oai.pmh.v2.OAIPMHtype;
import org.openarchives.oai.pmh.v2.ObjectFactory;

public class IdentifyRequestHandler implements OaiPmhRequestHandler<IdentifyRequest> {

  private static final String PROTOCOL_VERSION = "2.0";
  private static final String REPOSITORY_NAME = "NVA-OAI-PMH";
  private static final String CONTACT_AT_SIKT_NO = "kontakt@sikt.no";
  private static final int PAGE_SIZE_ONE = 1;
  private final URI endpointUri;
  private final ResourceRepository repository;

  public IdentifyRequestHandler(URI endpointUri, ResourceRepository repository) {
    this.endpointUri = endpointUri;
    this.repository = repository;
  }

  @Override
  public JAXBElement<OAIPMHtype> handleRequest(IdentifyRequest request) {
    var objectFactory = new ObjectFactory();
    var oaiResponse = baseResponse(objectFactory);
    var value = oaiResponse.getValue();
    value.getRequest().setVerb(request.getVerbType());

    var identify = objectFactory.createIdentifyType();
    identify.setBaseURL(endpointUri.toString());
    identify.setProtocolVersion(PROTOCOL_VERSION);
    identify.setEarliestDatestamp(findEarliestDatestamp());
    identify.setRepositoryName(REPOSITORY_NAME);
    identify.setGranularity(GranularityType.YYYY_MM_DD_THH_MM_SS_Z);
    identify.setDeletedRecord(DeletedRecordType.NO);
    identify.getAdminEmail().add(CONTACT_AT_SIKT_NO);

    value.setIdentify(identify);

    return oaiResponse;
  }

  private String findEarliestDatestamp() {
    var pagedResponse =
        repository.fetchInitialPage(
            OaiPmhDateTime.EMPTY_INSTANCE,
            OaiPmhDateTime.EMPTY_INSTANCE,
            SetSpec.EMPTY_INSTANCE,
            PAGE_SIZE_ONE);
    var earliestInstant =
        pagedResponse.totalSize() == 0
            ? defaultEarliestDatestamp()
            : extractEarliestDatestamp(pagedResponse.hits().getFirst());
    return OaiPmhDateTimeUtils.truncateToSeconds(earliestInstant.toString());
  }

  private Instant extractEarliestDatestamp(ResourceSearchResponse earliestHit) {
    var modifiedDate = earliestHit.recordMetadata().modifiedDate();
    return nonNull(modifiedDate) ? Instant.parse(modifiedDate) : defaultEarliestDatestamp();
  }

  private Instant defaultEarliestDatestamp() {
    return Instant.now();
  }
}
