package no.unit.nva.search.ticket;

import static no.unit.nva.common.Containers.container;
import static no.unit.nva.common.Containers.indexingClient;
import static no.unit.nva.constants.IndexMappingsAndSettings.TICKET_MAPPINGS;
import static no.unit.nva.indexing.testutils.MockedJwtProvider.setupMockedCachedJwtProvider;
import static no.unit.nva.search.ticket.TicketParameter.FROM;
import static no.unit.nva.search.ticket.TicketParameter.SIZE;
import static no.unit.nva.search.ticket.TicketType.FILES_APPROVAL_THESIS;
import static no.unit.nva.search.ticket.TicketType.PUBLISHING_REQUEST;
import static no.unit.nva.testutils.RandomDataGenerator.randomString;
import static no.unit.nva.testutils.RandomDataGenerator.randomUri;
import static nva.commons.apigateway.AccessRight.MANAGE_DEGREE;
import static nva.commons.apigateway.AccessRight.MANAGE_DOI;
import static nva.commons.core.attempt.Try.attempt;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.core.JsonProcessingException;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import no.unit.nva.commons.json.JsonUtils;
import no.unit.nva.constants.Words;
import no.unit.nva.identifiers.SortableIdentifier;
import no.unit.nva.indexingclient.models.EventConsumptionAttributes;
import no.unit.nva.indexingclient.models.IndexDocument;
import no.unit.nva.search.common.records.PagedSearch;
import nva.commons.apigateway.AccessRight;
import nva.commons.apigateway.RequestInfo;
import nva.commons.apigateway.exceptions.BadRequestException;
import nva.commons.apigateway.exceptions.UnauthorizedException;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.testcontainers.junit.jupiter.Testcontainers;

@Testcontainers
public class TicketClientTestV2 {

  private static final String indexName = randomString().toLowerCase(Locale.ROOT);
  private static TicketClient searchClient;

  @BeforeAll
  public static void setUp() {
    var cachedJwtProvider = setupMockedCachedJwtProvider();
    searchClient = new TicketClient(HttpClient.newHttpClient(), cachedJwtProvider);
  }

  @BeforeEach
  public void cleanIndex() {
    attempt(() -> indexingClient.deleteIndex(indexName));
  }

  @Test
  void shouldReturnFilesApprovalThesisWhenUserHasManageDegreeAccessRight()
      throws BadRequestException, UnauthorizedException, IOException {
    var topLevelId = randomUri();

    var json = ticketOfTypeWithTopLevelOrg(FILES_APPROVAL_THESIS, topLevelId);
    var document = toIndexDocument(json);
    createIndexAndAddDocumentsToIndex(document);

    var requestInfo = createRequest(topLevelId, MANAGE_DEGREE);

    var response = performSearch(requestInfo);

    assertEquals(1, response.hits().size());
  }

  @Test
  void shouldReturnFilesApprovalThesisOnlyWhenFilteringOnType()
      throws BadRequestException, UnauthorizedException, IOException {
    var topLevelId = randomUri();

    var filesApprovalThesisJson = ticketOfTypeWithTopLevelOrg(FILES_APPROVAL_THESIS, topLevelId);
    var publishingRequestJson = ticketOfTypeWithTopLevelOrg(PUBLISHING_REQUEST, topLevelId);
    var filesApprovalThesis = toIndexDocument(filesApprovalThesisJson);
    var publishingRequest = toIndexDocument(publishingRequestJson);
    createIndexAndAddDocumentsToIndex(filesApprovalThesis, publishingRequest);
    var requestInfo = createRequest(topLevelId, MANAGE_DEGREE, FILES_APPROVAL_THESIS.name());

    var response = performSearch(requestInfo);

    assertEquals(1, response.hits().size());
    assertTrue(response.hits().getFirst().toString().contains(FILES_APPROVAL_THESIS.toString()));
  }

  @Test
  void shouldReturnFilesApprovalTypeAggregation()
      throws BadRequestException, UnauthorizedException, IOException {
    var topLevelId = randomUri();

    var filesApprovalThesisJson = ticketOfTypeWithTopLevelOrg(FILES_APPROVAL_THESIS, topLevelId);
    var filesApprovalThesis = toIndexDocument(filesApprovalThesisJson);
    createIndexAndAddDocumentsToIndex(filesApprovalThesis);

    var requestInfo = createRequest(topLevelId, MANAGE_DEGREE, FILES_APPROVAL_THESIS.name());

    var response = performSearch(requestInfo);

    assertEquals(
        FILES_APPROVAL_THESIS.toString(), response.aggregations().get(Words.TYPE).getFirst().key());
  }

  @Test
  void shouldNotReturnFilesApprovalThesisWhenUserHasNoAccessRightToManageDegrees()
      throws BadRequestException, UnauthorizedException, IOException {
    var topLevelId = randomUri();

    var filesApprovalThesisJson = ticketOfTypeWithTopLevelOrg(FILES_APPROVAL_THESIS, topLevelId);
    var filesApprovalThesis = toIndexDocument(filesApprovalThesisJson);
    createIndexAndAddDocumentsToIndex(filesApprovalThesis);

    var requestInfo = createRequest(topLevelId, MANAGE_DOI, FILES_APPROVAL_THESIS.name());

    var response = performSearch(requestInfo);

    assertTrue(response.hits().isEmpty());
  }

  private static IndexDocument toIndexDocument(String json) throws JsonProcessingException {
    return new IndexDocument(
        new EventConsumptionAttributes(indexName, SortableIdentifier.next()),
        JsonUtils.dtoObjectMapper.readTree(json));
  }

  private static void createIndexAndAddDocumentsToIndex(IndexDocument... documents)
      throws IOException {
    indexingClient.createIndex(indexName, TICKET_MAPPINGS.asMap());
    Arrays.stream(documents).forEach(doc -> attempt(() -> indexingClient.addDocumentToIndex(doc)));
    indexingClient.refreshIndex(indexName);
  }

  private static String ticketOfTypeWithTopLevelOrg(TicketType type, URI topLevelId) {
    return """
                       {
             "type": "__TYPE__",
             "organization": {
               "id": "__TOP_LEVEL__"
             }
           }
           """
        .replace("__TYPE__", type.toString())
        .replace("__TOP_LEVEL__", topLevelId.toString());
  }

  private static PagedSearch performSearch(RequestInfo requestInfo)
      throws UnauthorizedException, BadRequestException {
    return TicketSearchQuery.builder()
        .withDockerHostUri(URI.create(container.getHttpHostAddress()))
        .fromRequestInfo(requestInfo)
        .withRequiredParameters(FROM, SIZE)
        .build()
        .withFilter()
        .fromRequestInfo(requestInfo)
        .doSearch(searchClient, indexName)
        .toPagedResponse();
  }

  private static RequestInfo createRequest(
      URI topLevelOrgIdentifier, AccessRight accessRight, String... types)
      throws UnauthorizedException {
    var requestInfo = mock(RequestInfo.class);
    when(requestInfo.getUserName()).thenReturn(randomString());
    when(requestInfo.getTopLevelOrgCristinId()).thenReturn(Optional.of(topLevelOrgIdentifier));
    when(requestInfo.getAccessRights()).thenReturn(List.of(accessRight));
    when(requestInfo.getHeaders()).thenReturn(Map.of("Authorization", randomString()));
    when(requestInfo.getMultiValueQueryStringParameters())
        .thenReturn(Map.of(Words.TYPE, Arrays.asList(types), "aggregation", List.of(Words.ALL)));
    return requestInfo;
  }
}
