package no.sikt.nva.oai.pmh.handler;

import static java.util.Objects.nonNull;
import static no.unit.nva.constants.Words.ZERO;
import static no.unit.nva.search.common.enums.PublicationStatus.PUBLISHED;
import static no.unit.nva.search.common.enums.PublicationStatus.PUBLISHED_METADATA;

import com.fasterxml.jackson.databind.JsonNode;
import jakarta.xml.bind.JAXBElement;
import java.math.BigInteger;
import java.time.ZonedDateTime;
import java.util.GregorianCalendar;
import java.util.List;
import javax.xml.datatype.DatatypeFactory;
import no.unit.nva.constants.Words;
import no.unit.nva.search.resource.ResourceClient;
import no.unit.nva.search.resource.ResourceParameter;
import no.unit.nva.search.resource.ResourceSearchQuery;
import no.unit.nva.search.resource.ResourceSort;
import no.unit.nva.search.resource.SimplifiedMutator;
import nva.commons.apigateway.exceptions.BadRequestException;
import org.openarchives.oai.pmh.v2.ListRecordsType;
import org.openarchives.oai.pmh.v2.OAIPMHtype;
import org.openarchives.oai.pmh.v2.ObjectFactory;
import org.openarchives.oai.pmh.v2.RecordType;
import org.openarchives.oai.pmh.v2.ResumptionTokenType;
import org.openarchives.oai.pmh.v2.VerbType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ListRecordsHandler extends AbstractOaiPmhMethodHandler {
  private static final Logger logger = LoggerFactory.getLogger(ListRecordsHandler.class);
  private static final int RESUMPTION_TOKEN_TTL_HOURS = 24;

  private final ObjectFactory objectFactory = new ObjectFactory();
  private final ResourceClient resourceClient;
  private final RecordTransformer recordTransformer;
  private final int batchSize;

  public ListRecordsHandler(
      ResourceClient resourceClient, RecordTransformer recordTransformer, int batchSize) {
    super();
    this.resourceClient = resourceClient;
    this.recordTransformer = recordTransformer;
    this.batchSize = batchSize;
  }

  public JAXBElement<OAIPMHtype> listRecords(
      String from, String until, String incomingEncodedResumptionToken, String metadataPrefix) {

    var searchResult = performSearch(from, until, incomingEncodedResumptionToken);
    var oaiResponse =
        createBaseResponse(from, until, incomingEncodedResumptionToken, metadataPrefix);

    var records = recordTransformer.transform(searchResult.hits);
    var listRecords =
        createListRecordsResponse(records, searchResult.totalSize(), from, until, metadataPrefix);

    oaiResponse.getValue().setListRecords(listRecords);
    return oaiResponse;
  }

  private SearchResult performSearch(
      String from, String until, String incomingEncodedResumptionToken) {
    var incomingResumptionToken = ResumptionToken.from(incomingEncodedResumptionToken);
    return incomingResumptionToken
        .map(resumptionToken -> doFollowUpSearch(until, resumptionToken))
        .orElseGet(() -> doInitialSearch(from, until));
  }

  private JAXBElement<OAIPMHtype> createBaseResponse(
      String from, String until, String incomingEncodedResumptionToken, String metadataPrefix) {
    var oaiResponse = baseResponse();
    populateListRecordsRequest(
        from, until, incomingEncodedResumptionToken, metadataPrefix, oaiResponse.getValue());
    return oaiResponse;
  }

  private ListRecordsType createListRecordsResponse(
      List<RecordType> records, int totalSize, String from, String until, String metadataPrefix) {

    var listRecords = objectFactory.createListRecordsType();
    listRecords.getRecord().addAll(records);

    String current = extractLastDateTime(records);
    if (shouldAddResumptionToken(current, totalSize)) {
      var resumptionTokenType =
          generateResumptionToken(from, until, metadataPrefix, current, totalSize);
      listRecords.setResumptionToken(resumptionTokenType);
    }

    return listRecords;
  }

  private boolean shouldAddResumptionToken(String current, int totalSize) {
    return nonNull(current) && totalSize >= batchSize;
  }

  private SearchResult doFollowUpSearch(String until, ResumptionToken resumptionToken) {
    var query = buildListRecordsPageQuery(resumptionToken.current(), until, batchSize);
    return doSearch(query);
  }

  private SearchResult doInitialSearch(String from, String until) {
    var query = buildListRecordsPageQuery(from, until, batchSize);
    return doSearch(query);
  }

  private SearchResult doSearch(ResourceSearchQuery query) {
    try {
      var response =
          query.doSearch(resourceClient, Words.RESOURCES).withMutators(new SimplifiedMutator());
      var mutatedHits = response.toMutatedHits();
      return new SearchResult(response.swsResponse().getTotalSize(), mutatedHits);
    } catch (RuntimeException e) {
      logger.error("Failed to search for records.", e);
      throw new ResourceSearchException("Error looking up records.", e);
    }
  }

  private static ResourceSearchQuery buildListRecordsPageQuery(
      String from, String until, int batchSize) {
    final ResourceSearchQuery query;
    try {
      query =
          ResourceSearchQuery.builder()
              .withParameter(ResourceParameter.AGGREGATION, Words.NONE)
              .withParameter(ResourceParameter.MODIFIED_SINCE, from)
              .withParameter(ResourceParameter.MODIFIED_BEFORE, until)
              .withParameter(ResourceParameter.FROM, ZERO)
              .withParameter(ResourceParameter.SIZE, Integer.toString(batchSize))
              .withParameter(ResourceParameter.SORT, ResourceSort.MODIFIED_DATE.asCamelCase())
              .withParameter(ResourceParameter.SORT_ORDER, "desc")
              .withAlwaysIncludedFields(SimplifiedMutator.getIncludedFields())
              .build()
              .withFilter()
              .requiredStatus(PUBLISHED, PUBLISHED_METADATA)
              .apply();
    } catch (BadRequestException e) {
      // should never happen unless query validation code is changed!
      logger.error("Failed to lookup initial page from OpenSearch during ListRecords.", e);
      throw new ResourceSearchException(
          "Error search for initial page of search results during ListRecords.", e);
    }
    return query;
  }

  private String extractLastDateTime(List<RecordType> records) {
    if (records.isEmpty()) {
      return null;
    } else {
      return records.getLast().getHeader().getDatestamp();
    }
  }

  private static void populateListRecordsRequest(
      String from,
      String until,
      String incomingResumptionToken,
      String metadataPrefix,
      OAIPMHtype oaiPmhType) {
    oaiPmhType.getRequest().setVerb(VerbType.LIST_RECORDS);
    oaiPmhType.getRequest().setResumptionToken(incomingResumptionToken);
    oaiPmhType.getRequest().setFrom(from);
    oaiPmhType.getRequest().setUntil(until);
    oaiPmhType.getRequest().setMetadataPrefix(metadataPrefix);
  }

  private record SearchResult(int totalSize, List<JsonNode> hits) {}

  private ResumptionTokenType generateResumptionToken(
      String from, String until, String metadataPrefix, String current, int totalSize) {
    var inTenMinutes =
        DatatypeFactory.newDefaultInstance()
            .newXMLGregorianCalendar(
                GregorianCalendar.from(ZonedDateTime.now().plusHours(RESUMPTION_TOKEN_TTL_HOURS)));

    var resumptionTokenType = objectFactory.createResumptionTokenType();
    var newResumptionToken = new ResumptionToken(from, until, metadataPrefix, current, totalSize);
    resumptionTokenType.setValue(newResumptionToken.getValue());
    resumptionTokenType.setExpirationDate(inTenMinutes);
    resumptionTokenType.setCompleteListSize(BigInteger.valueOf(totalSize));
    return resumptionTokenType;
  }
}
