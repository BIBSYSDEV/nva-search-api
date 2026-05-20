package no.sikt.nva.oai.pmh.handler.oaipmh.handlers;

import static java.util.Objects.nonNull;
import static no.sikt.nva.oai.pmh.handler.oaipmh.OaiPmhUtils.baseResponse;

import jakarta.xml.bind.JAXBElement;
import java.math.BigInteger;
import java.time.ZonedDateTime;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.Optional;
import javax.xml.datatype.DatatypeFactory;
import no.sikt.nva.oai.pmh.handler.oaipmh.RecordTransformer;
import no.sikt.nva.oai.pmh.handler.oaipmh.ResumptionToken;
import no.sikt.nva.oai.pmh.handler.oaipmh.request.ListRecordsRequest;
import no.sikt.nva.oai.pmh.handler.repository.PagedResponse;
import no.sikt.nva.oai.pmh.handler.repository.ResourceRepository;
import org.openarchives.oai.pmh.v2.ListRecordsType;
import org.openarchives.oai.pmh.v2.OAIPMHtype;
import org.openarchives.oai.pmh.v2.ObjectFactory;
import org.openarchives.oai.pmh.v2.RecordType;
import org.openarchives.oai.pmh.v2.ResumptionTokenType;
import org.openarchives.oai.pmh.v2.VerbType;

public class ListRecordsRequestHandler implements OaiPmhRequestHandler<ListRecordsRequest> {

  private static final int RESUMPTION_TOKEN_TTL_HOURS = 24;
  private static final String SEARCH_AFTER_SEPARATOR = ",";

  private final ResourceRepository repository;
  private final RecordTransformer recordTransformer;
  private final int batchSize;

  public ListRecordsRequestHandler(
      ResourceRepository repository, RecordTransformer recordTransformer, int batchSize) {
    super();
    this.repository = repository;
    this.recordTransformer = recordTransformer;
    this.batchSize = batchSize;
  }

  @Override
  public JAXBElement<OAIPMHtype> handleRequest(ListRecordsRequest request) {
    var objectFactory = new ObjectFactory();

    var oaiResponse = createBaseResponse(request, objectFactory);
    var searchResult = performSearch(request);
    var searchAfterValue = extractSearchAfterFromLastHit(searchResult).orElse(null);
    var records = recordTransformer.transform(searchResult.hits());
    var listRecords =
        createListRecordsResponse(
            records, searchAfterValue, searchResult.totalSize(), request, objectFactory);

    oaiResponse.getValue().setListRecords(listRecords);
    return oaiResponse;
  }

  private Optional<String> extractSearchAfterFromLastHit(PagedResponse pagedResponse) {
    if (pagedResponse.hits().isEmpty()) {
      return Optional.empty();
    }

    var lastHit = pagedResponse.hits().getLast();
    var modifiedDate = lastHit.recordMetadata().modifiedDate();
    var identifier = lastHit.identifier();
    return Optional.of(modifiedDate + SEARCH_AFTER_SEPARATOR + identifier);
  }

  private PagedResponse performSearch(ListRecordsRequest request) {
    var incomingResumptionToken = request.getResumptionToken();
    var setSpec = request.getSetSpec();
    return nonNull(incomingResumptionToken)
        ? doFollowUpSearch(incomingResumptionToken)
        : repository.fetchInitialPage(request.getFrom(), request.getUntil(), setSpec, batchSize);
  }

  private JAXBElement<OAIPMHtype> createBaseResponse(
      ListRecordsRequest listRecordsRequest, ObjectFactory objectFactory) {
    var oaiResponse = baseResponse(objectFactory);
    populateListRecordsRequest(listRecordsRequest, oaiResponse.getValue());
    return oaiResponse;
  }

  private ListRecordsType createListRecordsResponse(
      List<RecordType> records,
      String searchAfterValue,
      int totalSize,
      ListRecordsRequest request,
      ObjectFactory objectFactory) {

    var listRecords = objectFactory.createListRecordsType();
    listRecords.getRecord().addAll(records);

    var isLastPage = records.size() < batchSize;
    var nextResumptionToken =
        isLastPage
            ? createEmptyResumptionToken(totalSize, objectFactory)
            : createNewResumptionToken(request, searchAfterValue, totalSize, objectFactory);
    listRecords.setResumptionToken(nextResumptionToken);

    return listRecords;
  }

  private PagedResponse doFollowUpSearch(ResumptionToken resumptionToken) {
    var pagedResponse =
        repository.fetchNextPage(
            resumptionToken.searchAfter(),
            resumptionToken.originalRequest().getUntil(),
            resumptionToken.originalRequest().getSetSpec(),
            batchSize);
    return new PagedResponse(resumptionToken.totalSize(), pagedResponse.hits());
  }

  private static void populateListRecordsRequest(
      ListRecordsRequest request, OAIPMHtype oaiPmhType) {
    var resumptionTokenValue =
        nonNull(request.getResumptionToken()) ? request.getResumptionToken().getValue() : null;
    oaiPmhType.getRequest().setVerb(VerbType.LIST_RECORDS);
    oaiPmhType.getRequest().setResumptionToken(resumptionTokenValue);
    oaiPmhType.getRequest().setFrom(request.getFrom().getOriginalSource().orElse(null));
    oaiPmhType.getRequest().setUntil(request.getUntil().getOriginalSource().orElse(null));
    oaiPmhType.getRequest().setSet(request.getSetSpec().getValue().orElse(null));
    oaiPmhType.getRequest().setMetadataPrefix(request.getMetadataPrefix().getPrefix());
  }

  private static ResumptionTokenType createEmptyResumptionToken(
      int totalSize, ObjectFactory objectFactory) {
    var resumptionTokenType = objectFactory.createResumptionTokenType();
    resumptionTokenType.setCompleteListSize(BigInteger.valueOf(totalSize));
    return resumptionTokenType;
  }

  private static ResumptionTokenType createNewResumptionToken(
      ListRecordsRequest originalRequest,
      String searchAfter,
      int totalSize,
      ObjectFactory objectFactory) {
    var inTenMinutes =
        DatatypeFactory.newDefaultInstance()
            .newXMLGregorianCalendar(
                GregorianCalendar.from(ZonedDateTime.now().plusHours(RESUMPTION_TOKEN_TTL_HOURS)));

    var resumptionTokenType = objectFactory.createResumptionTokenType();
    var newResumptionToken = new ResumptionToken(originalRequest, searchAfter, totalSize);
    resumptionTokenType.setValue(newResumptionToken.getValue());
    resumptionTokenType.setExpirationDate(inTenMinutes);
    resumptionTokenType.setCompleteListSize(BigInteger.valueOf(totalSize));
    return resumptionTokenType;
  }
}
