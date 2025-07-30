package no.sikt.nva.oai.pmh.handler.oaipmh;

import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;
import static no.sikt.nva.oai.pmh.handler.oaipmh.OaiPmhUtils.baseResponse;
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
import no.sikt.nva.oai.pmh.handler.oaipmh.SetSpec.SetRoot;
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

public class ListRecords {

  private static final Logger logger = LoggerFactory.getLogger(ListRecords.class);
  private static final int RESUMPTION_TOKEN_TTL_HOURS = 24;
  public static final String MODIFIED_DATE_ASCENDING =
      ResourceSort.MODIFIED_DATE.asCamelCase() + ":asc";

  private final ResourceClient resourceClient;
  private final RecordTransformer recordTransformer;
  private final int batchSize;

  public ListRecords(
      ResourceClient resourceClient, RecordTransformer recordTransformer, int batchSize) {
    super();
    this.resourceClient = resourceClient;
    this.recordTransformer = recordTransformer;
    this.batchSize = batchSize;
  }

  public JAXBElement<OAIPMHtype> listRecords(ListRecordsRequest request) {
    var objectFactory = new ObjectFactory();

    var oaiResponse = createBaseResponse(request, objectFactory);
    var searchResult = performSearch(request);
    var records = recordTransformer.transform(searchResult.hits);
    var listRecords =
        createListRecordsResponse(records, searchResult.totalSize(), request, objectFactory);

    oaiResponse.getValue().setListRecords(listRecords);
    return oaiResponse;
  }

  private SearchResult performSearch(ListRecordsRequest request) {
    var incomingResumptionToken = request.getResumptionToken();
    var setSpec = request.getSetSpec();
    return nonNull(incomingResumptionToken)
        ? doFollowUpSearch(incomingResumptionToken)
        : doInitialSearch(request.getFrom().asString(), request.getUntil().asString(), setSpec);
  }

  private JAXBElement<OAIPMHtype> createBaseResponse(
      ListRecordsRequest listRecordsRequest, ObjectFactory objectFactory) {
    var oaiResponse = baseResponse(objectFactory);
    var metadataPrefix = listRecordsRequest.getMetadataPrefix();
    populateListRecordsRequest(
        listRecordsRequest.getFrom().asString(),
        listRecordsRequest.getUntil().asString(),
        nonNull(listRecordsRequest.getResumptionToken())
            ? listRecordsRequest.getResumptionToken().getValue()
            : null,
        metadataPrefix.getPrefix(),
        oaiResponse.getValue());
    return oaiResponse;
  }

  private ListRecordsType createListRecordsResponse(
      List<RecordType> records,
      int totalSize,
      ListRecordsRequest request,
      ObjectFactory objectFactory) {

    var listRecords = objectFactory.createListRecordsType();
    listRecords.getRecord().addAll(records);

    var current = extractLastDateTime(records);
    var pageSize = records.size();
    if (shouldAddResumptionToken(current, pageSize)) {
      var resumptionTokenType = generateResumptionToken(request, current, totalSize, objectFactory);
      listRecords.setResumptionToken(resumptionTokenType);
    }

    return listRecords;
  }

  private boolean shouldAddResumptionToken(String current, int totalSize) {
    return nonNull(current) && totalSize >= batchSize;
  }

  private SearchResult doFollowUpSearch(ResumptionToken resumptionToken) {
    var setSpec = resumptionToken.originalRequest().getSetSpec();
    var query =
        buildListRecordsPageQuery(
            resumptionToken.current(),
            resumptionToken.originalRequest().getUntil().asString(),
            setSpec,
            batchSize);
    return doSearch(query, resumptionToken.totalSize());
  }

  private SearchResult doInitialSearch(String from, String until, SetSpec setSpec) {
    var query = buildListRecordsPageQuery(from, until, setSpec, batchSize);
    return doSearch(query, null);
  }

  private SearchResult doSearch(ResourceSearchQuery query, Integer incomingTotalSize) {
    try {
      var response =
          query.doSearch(resourceClient, Words.RESOURCES).withMutators(new SimplifiedMutator());
      var mutatedHits = response.toMutatedHits();
      var pageSize = mutatedHits.size();
      var remainingHits = response.swsResponse().getTotalSize();
      var totalSize = isNull(incomingTotalSize) ? remainingHits : incomingTotalSize;
      return new SearchResult(totalSize, pageSize, mutatedHits);
    } catch (RuntimeException e) {
      logger.error("Failed to search for records.", e);
      throw new ResourceSearchException("Error looking up records.", e);
    }
  }

  private static ResourceSearchQuery buildListRecordsPageQuery(
      String from, String until, SetSpec setSpec, int batchSize) {
    final ResourceSearchQuery query;
    try {
      var builder =
          ResourceSearchQuery.builder()
              .withParameter(ResourceParameter.AGGREGATION, Words.NONE)
              .withParameter(ResourceParameter.FROM, ZERO)
              .withParameter(ResourceParameter.SIZE, Integer.toString(batchSize))
              .withParameter(ResourceParameter.SORT, MODIFIED_DATE_ASCENDING);
      if (nonNull(from)) {
        builder.withParameter(ResourceParameter.MODIFIED_SINCE, from);
      }
      if (nonNull(until)) {
        builder.withParameter(ResourceParameter.MODIFIED_BEFORE, until);
      }
      if (nonNull(setSpec)
          && SetRoot.RESOURCE_TYPE_GENERAL.equals(setSpec.root())
          && setSpec.children().length > 0) {
        builder.withParameter(ResourceParameter.INSTANCE_TYPE, setSpec.children()[0]);
      }
      query =
          builder
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

  private record SearchResult(int totalSize, int pageSize, List<JsonNode> hits) {}

  private ResumptionTokenType generateResumptionToken(
      ListRecordsRequest originalRequest,
      String current,
      int totalSize,
      ObjectFactory objectFactory) {
    var inTenMinutes =
        DatatypeFactory.newDefaultInstance()
            .newXMLGregorianCalendar(
                GregorianCalendar.from(ZonedDateTime.now().plusHours(RESUMPTION_TOKEN_TTL_HOURS)));

    var resumptionTokenType = objectFactory.createResumptionTokenType();
    var newResumptionToken = new ResumptionToken(originalRequest, current, totalSize);
    resumptionTokenType.setValue(newResumptionToken.getValue());
    resumptionTokenType.setExpirationDate(inTenMinutes);
    resumptionTokenType.setCompleteListSize(BigInteger.valueOf(totalSize));
    return resumptionTokenType;
  }
}
