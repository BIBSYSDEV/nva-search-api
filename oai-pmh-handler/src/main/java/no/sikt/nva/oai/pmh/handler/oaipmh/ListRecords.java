package no.sikt.nva.oai.pmh.handler.oaipmh;

import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;
import static no.sikt.nva.oai.pmh.handler.oaipmh.OaiPmhUtils.baseResponse;
import static no.unit.nva.constants.Words.ZERO;
import static no.unit.nva.search.common.enums.PublicationStatus.PUBLISHED;
import static no.unit.nva.search.common.enums.PublicationStatus.PUBLISHED_METADATA;

import com.fasterxml.jackson.databind.JsonNode;
import com.google.common.html.HtmlEscapers;
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
import org.openarchives.oai.pmh.v2.OAIPMHerrorcodeType;
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

  public JAXBElement<OAIPMHtype> listRecords(OaiPmhRequest request) {
    var objectFactory = new ObjectFactory();

    var oaiResponse = createBaseResponse(request, objectFactory);
    try {
      var ignored =
          nonNull(request.getMetadataPrefix())
              ? MetadataPrefix.fromPrefix(request.getMetadataPrefix())
              : MetadataPrefix.OAI_DC;
    } catch (MetadataPrefixNotSupportedException e) {
      return reportCannotDisseminateFormatError(request, objectFactory, oaiResponse);
    }
    var searchResult = performSearch(request);

    var records = recordTransformer.transform(searchResult.hits);
    var listRecords =
        createListRecordsResponse(records, searchResult.totalSize(), request, objectFactory);

    oaiResponse.getValue().setListRecords(listRecords);
    return oaiResponse;
  }

  private static JAXBElement<OAIPMHtype> reportCannotDisseminateFormatError(
      OaiPmhRequest request, ObjectFactory objectFactory, JAXBElement<OAIPMHtype> oaiResponse) {
    var errorType = objectFactory.createOAIPMHerrorType();
    errorType.setCode(OAIPMHerrorcodeType.CANNOT_DISSEMINATE_FORMAT);
    errorType.setValue(HtmlEscapers.htmlEscaper().escape(request.getMetadataPrefix()));
    oaiResponse.getValue().getError().add(errorType);
    return oaiResponse;
  }

  private SearchResult performSearch(OaiPmhRequest request) {
    var incomingResumptionToken =
        ResumptionToken.from(VerbType.LIST_RECORDS, request.getResumptionToken());
    return incomingResumptionToken
        .map(resumptionToken -> doFollowUpSearch(request.getUntil(), resumptionToken))
        .orElseGet(() -> doInitialSearch(request.getFrom(), request.getUntil()));
  }

  private JAXBElement<OAIPMHtype> createBaseResponse(
      OaiPmhRequest context, ObjectFactory objectFactory) {
    var oaiResponse = baseResponse(objectFactory);
    var metadataPrefix = context.getMetadataPrefix();
    populateListRecordsRequest(
        context.getFrom(),
        context.getUntil(),
        context.getResumptionToken(),
        metadataPrefix,
        oaiResponse.getValue());
    return oaiResponse;
  }

  private ListRecordsType createListRecordsResponse(
      List<RecordType> records, int totalSize, OaiPmhRequest request, ObjectFactory objectFactory) {

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

  private SearchResult doFollowUpSearch(String until, ResumptionToken resumptionToken) {
    var query = buildListRecordsPageQuery(resumptionToken.current(), until, batchSize);
    return doSearch(query, resumptionToken.totalSize());
  }

  private SearchResult doInitialSearch(String from, String until) {
    var query = buildListRecordsPageQuery(from, until, batchSize);
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
              .withParameter(ResourceParameter.SORT, MODIFIED_DATE_ASCENDING)
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
      OaiPmhRequest originalRequest, String current, int totalSize, ObjectFactory objectFactory) {
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
