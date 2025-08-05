package no.sikt.nva.oai.pmh.handler.oaipmh;

import static java.util.Objects.nonNull;
import static no.sikt.nva.oai.pmh.handler.oaipmh.OaiPmhUtils.baseResponse;
import static no.unit.nva.constants.Words.ZERO;
import static no.unit.nva.search.common.enums.PublicationStatus.PUBLISHED;
import static no.unit.nva.search.common.enums.PublicationStatus.PUBLISHED_METADATA;

import com.fasterxml.jackson.databind.JsonNode;
import jakarta.xml.bind.JAXBElement;
import java.net.URI;
import java.time.Instant;
import no.sikt.nva.oai.pmh.handler.oaipmh.request.IdentifyRequest;
import no.unit.nva.commons.json.JsonUtils;
import no.unit.nva.constants.Words;
import no.unit.nva.search.resource.ResourceClient;
import no.unit.nva.search.resource.ResourceParameter;
import no.unit.nva.search.resource.ResourceSearchQuery;
import no.unit.nva.search.resource.SimplifiedMutator;
import no.unit.nva.search.resource.response.ResourceSearchResponse;
import nva.commons.apigateway.exceptions.BadRequestException;
import org.openarchives.oai.pmh.v2.DeletedRecordType;
import org.openarchives.oai.pmh.v2.GranularityType;
import org.openarchives.oai.pmh.v2.OAIPMHtype;
import org.openarchives.oai.pmh.v2.ObjectFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class IdentifyRequestHandler implements OaiPmhRequestHandler<IdentifyRequest> {
  private static final Logger LOGGER = LoggerFactory.getLogger(IdentifyRequestHandler.class);
  private static final String PROTOCOL_VERSION = "2.0";
  private static final String REPOSITORY_NAME = "NVA-OAI-PMH";
  private static final String CONTACT_AT_SIKT_NO = "kontakt@sikt.no";
  private final URI endpointUri;
  private final ResourceClient resourceClient;

  public IdentifyRequestHandler(URI endpointUri, ResourceClient resourceClient) {
    this.endpointUri = endpointUri;
    this.resourceClient = resourceClient;
  }

  @Override
  public JAXBElement<OAIPMHtype> handleRequest(IdentifyRequest request) {
    var objectFactory = new ObjectFactory();
    var oaiResponse = baseResponse(objectFactory);
    var value = oaiResponse.getValue();
    value.getRequest().setVerb(request.getVerbType());

    var earliestDatestamp = findEarliestDatestamp();
    var identify = objectFactory.createIdentifyType();
    identify.setBaseURL(endpointUri.toString());
    identify.setProtocolVersion(PROTOCOL_VERSION);
    identify.setEarliestDatestamp(earliestDatestamp);
    identify.setRepositoryName(REPOSITORY_NAME);
    identify.setGranularity(GranularityType.YYYY_MM_DD_THH_MM_SS_Z);
    identify.setDeletedRecord(DeletedRecordType.NO);
    identify.getAdminEmail().add(CONTACT_AT_SIKT_NO);

    value.setIdentify(identify);

    return oaiResponse;
  }

  private String findEarliestDatestamp() {

    try {
      var query =
          ResourceSearchQuery.builder()
              .withParameter(ResourceParameter.AGGREGATION, Words.NONE)
              .withParameter(ResourceParameter.FROM, ZERO)
              .withParameter(ResourceParameter.SIZE, "1")
              .withParameter(ResourceParameter.SORT, MODIFIED_DATE_ASCENDING)
              .withAlwaysIncludedFields(SimplifiedMutator.getIncludedFields())
              .build()
              .withFilter()
              .requiredStatus(PUBLISHED, PUBLISHED_METADATA)
              .apply();
      var response =
          query.doSearch(resourceClient, Words.RESOURCES).withMutators(new SimplifiedMutator());
      var hits = response.toMutatedHits();
      var earliestInstant =
          hits.isEmpty() ? defaultEarliestDatestamp() : extractEarliestDatestamp(hits.getFirst());
      return OaiPmhDateTimeUtils.truncateToSeconds(earliestInstant.toString());
    } catch (RuntimeException | BadRequestException e) {
      LOGGER.error("Failed to search for earliest datestamp.", e);
      throw new ResourceSearchException("Failed to search for earliest datestamp.", e);
    }
  }

  private Instant extractEarliestDatestamp(JsonNode earliestHit) {
    var simplifiedHit =
        JsonUtils.dtoObjectMapper.convertValue(earliestHit, ResourceSearchResponse.class);
    var modifiedDate = simplifiedHit.recordMetadata().modifiedDate();
    return nonNull(modifiedDate) ? Instant.parse(modifiedDate) : defaultEarliestDatestamp();
  }

  private Instant defaultEarliestDatestamp() {
    return Instant.now();
  }
}
