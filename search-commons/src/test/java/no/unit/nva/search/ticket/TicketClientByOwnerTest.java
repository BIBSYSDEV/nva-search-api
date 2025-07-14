package no.unit.nva.search.ticket;

import static no.unit.nva.common.Containers.container;
import static no.unit.nva.indexing.testutils.MockedJwtProvider.setupMockedCachedJwtProvider;
import static no.unit.nva.search.ticket.TicketParameter.AGGREGATION;
import static no.unit.nva.search.ticket.TicketParameter.FROM;
import static no.unit.nva.search.ticket.TicketParameter.OWNER;
import static no.unit.nva.search.ticket.TicketParameter.SIZE;
import static no.unit.nva.search.ticket.TicketParameter.TYPE;
import static nva.commons.core.attempt.Try.attempt;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.core.JsonProcessingException;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.http.HttpClient;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import no.unit.nva.commons.json.JsonUtils;
import no.unit.nva.identifiers.SortableIdentifier;
import no.unit.nva.indexingclient.IndexingClient;
import no.unit.nva.indexingclient.models.EventConsumptionAttributes;
import no.unit.nva.indexingclient.models.IndexDocument;
import no.unit.nva.indexingclient.models.RestHighLevelClientWrapper;
import no.unit.nva.search.common.records.SwsResponse.HitsInfo.Hit;
import nva.commons.apigateway.AccessRight;
import nva.commons.apigateway.RequestInfo;
import nva.commons.apigateway.exceptions.BadRequestException;
import nva.commons.apigateway.exceptions.UnauthorizedException;
import org.apache.hc.core5.http.HttpHost;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.opensearch.client.RestClient;
import org.testcontainers.junit.jupiter.Testcontainers;

@Testcontainers
class TicketClientByOwnerTest {
  private static final String INDEX_NAME =
      TicketClientByOwnerTest.class.getSimpleName().toLowerCase(Locale.ROOT);
  private static final String MY_ORG_IDENTIFIER = "1.0.0.0";
  private static final String OTHER_ORG_IDENTIFIER = "2.0.0.0";
  private static final String USERNAME = "me@" + MY_ORG_IDENTIFIER;

  private static final String TICKET_TEMPLATE =
      """
{
  "type": "@@TICKET_TYPE@@",
  "identifier": "@@TICKET_IDENTIFIER@@",
  "owner": {
    "username": "@@USERNAME@@"
  },
  "organization": {
    "id": "https://api.unittests.nva.aws.unit.no/cristin/organization/@@ORG_IDENTIFIER@@",
    "identifier": "@@ORG_IDENTIFIER@@"
  }
}
""";
  private static TicketClient ticketClient;
  private static final Map<String, Set<String>> EXPECTED_HITS_PER_USERNAME = new HashMap<>();

  @BeforeAll
  static void beforeAll() throws IOException {
    var cachedJwtProvider = setupMockedCachedJwtProvider();
    ticketClient = new TicketClient(HttpClient.newHttpClient(), cachedJwtProvider);

    try {
      var restClientBuilder = RestClient.builder(HttpHost.create(container.getHttpHostAddress()));

      var restHighLevelClientWrapper = new RestHighLevelClientWrapper(restClientBuilder);
      var indexingClient = new IndexingClient(restHighLevelClientWrapper, cachedJwtProvider);

      indexingClient.createIndex(INDEX_NAME);
      indexDocuments()
          .forEach(
              indexDocument ->
                  attempt(() -> indexingClient.addDocumentToIndex(indexDocument)).orElseThrow());
      indexingClient.refreshIndex(INDEX_NAME);
    } catch (URISyntaxException e) {
      throw new IllegalArgumentException(e);
    }
  }

  private static String ticketJsonFromTemplate(
      SortableIdentifier sortableIdentifier,
      TicketType ticketType,
      String username,
      String organizationIdentifier) {
    return TICKET_TEMPLATE
        .replaceAll("@@TICKET_IDENTIFIER@@", sortableIdentifier.toString())
        .replaceAll("@@TICKET_TYPE@@", ticketType.toString())
        .replaceAll("@@USERNAME@@", username)
        .replaceAll("@@ORG_IDENTIFIER@@", organizationIdentifier);
  }

  private static IndexDocument indexDocument(
      String username,
      TicketType ticketType,
      String organizationIdentifier,
      boolean expectedForUser)
      throws JsonProcessingException {
    var identifier = SortableIdentifier.next();
    if (expectedForUser) {
      EXPECTED_HITS_PER_USERNAME
          .computeIfAbsent(username, s -> new HashSet<>())
          .add(identifier.toString());
    }
    return new IndexDocument(
        new EventConsumptionAttributes(INDEX_NAME, identifier),
        JsonUtils.dtoObjectMapper.readTree(
            ticketJsonFromTemplate(identifier, ticketType, username, organizationIdentifier)));
  }

  private static Stream<IndexDocument> indexDocuments() throws JsonProcessingException {
    return Stream.of(
        indexDocument(USERNAME, TicketType.PUBLISHING_REQUEST, MY_ORG_IDENTIFIER, true),
        indexDocument(USERNAME, TicketType.FILES_APPROVAL_THESIS, MY_ORG_IDENTIFIER, true),
        indexDocument(USERNAME, TicketType.GENERAL_SUPPORT_CASE, MY_ORG_IDENTIFIER, true),
        indexDocument(USERNAME, TicketType.UNPUBLISH_REQUEST, MY_ORG_IDENTIFIER, false),
        indexDocument(USERNAME, TicketType.GENERAL_SUPPORT_CASE, OTHER_ORG_IDENTIFIER, true),
        indexDocument("someone@else", TicketType.GENERAL_SUPPORT_CASE, MY_ORG_IDENTIFIER, false));
  }

  @Test
  void shouldReturnTicketsOwnedByUser() throws BadRequestException, UnauthorizedException {
    var hostAddress = URI.create(container.getHttpHostAddress());
    var requestInfo = Mockito.mock(RequestInfo.class);

    when(requestInfo.getUserName()).thenReturn(USERNAME);
    when(requestInfo.getTopLevelOrgCristinId())
        .thenReturn(
            Optional.of(
                URI.create(
                    "https://api.unittests.nva.aws.unit"
                        + ".no/cristin/organization/"
                        + MY_ORG_IDENTIFIER)));
    when(requestInfo.getMultiValueQueryStringParameters())
        .thenReturn(
            Map.of(
                OWNER.asLowerCase(), List.of(USERNAME),
                TYPE.asLowerCase(),
                    List.of(
                        "doiRequest,generalSupportCase,publishingRequest,FilesApprovalThesis")));
    when(requestInfo.getAccessRights()).thenReturn(List.of(AccessRight.MANAGE_OWN_RESOURCES));

    var response =
        TicketSearchQuery.builder()
            .fromRequestInfo(requestInfo)
            .withDockerHostUri(hostAddress)
            .withRequiredParameters(FROM, SIZE, AGGREGATION)
            .build()
            .withFilter()
            .fromRequestInfo(requestInfo)
            .doSearch(ticketClient, INDEX_NAME);

    var actualIdentifiers =
        response.swsResponse().hits().hits().stream()
            .map(Hit::_source)
            .map(node -> node.get("identifier").asText())
            .collect(Collectors.toSet());
    assertThat(actualIdentifiers).containsAll(EXPECTED_HITS_PER_USERNAME.get(USERNAME));
  }
}
