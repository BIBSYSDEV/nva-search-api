package no.unit.nva.search.ticket;

import static no.unit.nva.common.Containers.container;
import static no.unit.nva.common.Containers.indexingClient;
import static no.unit.nva.common.TestConstants.DELAY_AFTER_INDEXING;
import static no.unit.nva.constants.IndexMappingsAndSettings.TICKET_MAPPINGS;
import static no.unit.nva.indexing.testutils.MockedJwtProvider.setupMockedCachedJwtProvider;
import static no.unit.nva.search.ticket.TicketParameter.AGGREGATION;
import static no.unit.nva.search.ticket.TicketParameter.FROM;
import static no.unit.nva.search.ticket.TicketParameter.SIZE;
import static no.unit.nva.testutils.RandomDataGenerator.randomString;
import static no.unit.nva.testutils.RandomDataGenerator.randomUri;
import static nva.commons.apigateway.AccessRight.MANAGE_DOI;
import static nva.commons.apigateway.AccessRight.MANAGE_PUBLISHING_REQUESTS;
import static nva.commons.apigateway.AccessRight.SUPPORT;
import static nva.commons.core.attempt.Try.attempt;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import no.unit.nva.commons.json.JsonUtils;
import no.unit.nva.constants.Words;
import no.unit.nva.identifiers.SortableIdentifier;
import no.unit.nva.indexingclient.models.EventConsumptionAttributes;
import no.unit.nva.indexingclient.models.IndexDocument;
import nva.commons.apigateway.RequestInfo;
import nva.commons.apigateway.ViewingScope;
import nva.commons.apigateway.exceptions.BadRequestException;
import nva.commons.apigateway.exceptions.UnauthorizedException;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.testcontainers.junit.jupiter.Testcontainers;

@Testcontainers
public class TicketClientViewingScopeTest {

  private static final String indexName = "tickets";
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
  void shouldReturnTicketWithTheSameTopLevelOrgAsUserThatMakesRequest()
      throws BadRequestException, UnauthorizedException, IOException, InterruptedException {
    var topLevelId = randomUri();

    var json =
        """
                    {
          "type": "PublishingRequest",
          "organization": {
            "id": "__TOP_LEVEL__",
            "identifier": "__TOP_LEVEL__",
            "partOf": [
              {
                "id": "__TOP_LEVEL__",
                "identifier": "__TOP_LEVEL__"
              }
            ]
          }
        }
        """
            .replace("__TOP_LEVEL__", topLevelId.toString());
    var document =
        new IndexDocument(
            new EventConsumptionAttributes(indexName, SortableIdentifier.next()),
            JsonUtils.dtoObjectMapper.readTree(json));
    indexingClient.createIndex(indexName, TICKET_MAPPINGS.asMap());
    indexingClient.addDocumentToIndex(document);
    Thread.sleep(DELAY_AFTER_INDEXING);

    var requestInfo = requestInfoTopLevelOrg(topLevelId);

    var response =
        TicketSearchQuery.builder()
            .withDockerHostUri(URI.create(container.getHttpHostAddress()))
            .fromRequestInfo(requestInfo)
            .withRequiredParameters(FROM, SIZE, AGGREGATION)
            .build()
            .withFilter()
            .fromRequestInfo(requestInfo)
            .doSearch(searchClient, Words.RESOURCES)
            .toPagedResponse();

    assertEquals(1, response.hits().size());
  }

  @Test
  void
      shouldReturnTicketsWhenQueryParamContainsSubUnitOfTopLevelAndTicketOrganizationItThatSubunit()
          throws BadRequestException, UnauthorizedException, IOException, InterruptedException {
    var topLevelOrg = randomUri();
    var subunit = randomString();

    var json =
        """
                    {
          "type": "PublishingRequest",
          "organization": {
            "id": "__SUBUNIT__",
            "identifier": "__SUBUNIT__",
            "partOf": [
              {
                "id": "__TOP_LEVEL__",
                "identifier": "__TOP_LEVEL__"
              }
            ]
          }
        }
        """
            .replace("__SUBUNIT__", subunit)
            .replace("__TOP_LEVEL__", topLevelOrg.toString());
    var document =
        new IndexDocument(
            new EventConsumptionAttributes(indexName, SortableIdentifier.next()),
            JsonUtils.dtoObjectMapper.readTree(json));
    indexingClient.createIndex(indexName, TICKET_MAPPINGS.asMap());
    indexingClient.addDocumentToIndex(document);
    Thread.sleep(DELAY_AFTER_INDEXING);

    var requestInfo =
        requestInfoWithQueryParams(topLevelOrg, Map.of("organizationId", List.of(subunit)));

    var response =
        TicketSearchQuery.builder()
            .withDockerHostUri(URI.create(container.getHttpHostAddress()))
            .fromRequestInfo(requestInfo)
            .withRequiredParameters(FROM, SIZE, AGGREGATION)
            .build()
            .withFilter()
            .fromRequestInfo(requestInfo)
            .doSearch(searchClient, Words.RESOURCES)
            .toPagedResponse();

    assertEquals(1, response.hits().size());
  }

  @Test
  void shouldNotReturnTicketsWhenOrganizationsNotMatch()
      throws BadRequestException, UnauthorizedException, IOException, InterruptedException {

    var subunit = randomString();
    var json =
        """
                    {
          "type": "PublishingRequest",
          "organization": {
            "id": "__SUBUNIT__",
            "identifier": "__SUBUNIT__",
            "partOf": [
              {
                "id": "__TOP_LEVEL__",
                "identifier": "__TOP_LEVEL__"
              }
            ]
          }
        }
        """
            .replace("__SUBUNIT__", subunit)
            .replace("__TOP_LEVEL__", randomString());
    var document =
        new IndexDocument(
            new EventConsumptionAttributes(indexName, SortableIdentifier.next()),
            JsonUtils.dtoObjectMapper.readTree(json));
    indexingClient.createIndex(indexName, TICKET_MAPPINGS.asMap());
    indexingClient.addDocumentToIndex(document);
    Thread.sleep(DELAY_AFTER_INDEXING);

    var requestInfo =
        requestInfoWithQueryParams(randomUri(), Map.of("organizationId", List.of(subunit)));

    var response =
        TicketSearchQuery.builder()
            .withDockerHostUri(URI.create(container.getHttpHostAddress()))
            .fromRequestInfo(requestInfo)
            .withRequiredParameters(FROM, SIZE, AGGREGATION)
            .build()
            .withFilter()
            .fromRequestInfo(requestInfo)
            .doSearch(searchClient, Words.RESOURCES)
            .toPagedResponse();

    assertEquals(0, response.hits().size());
  }

  @Test
  void shouldReturnOnlyTicketsWithSubunitFromQueryParamsWhenProvided()
      throws BadRequestException, UnauthorizedException, IOException, InterruptedException {
    indexingClient.createIndex(indexName, TICKET_MAPPINGS.asMap());

    var subunit1 = randomString();
    var topLevel = randomString();
    var json1 =
        """
                    {
          "type": "PublishingRequest",
          "organization": {
            "id": "__SUBUNIT__",
            "identifier": "__SUBUNIT__",
            "partOf": [
              {
                "id": "__TOP_LEVEL__",
                "identifier": "__TOP_LEVEL__"
              }
            ]
          }
        }
        """
            .replace("__SUBUNIT__", subunit1.toString())
            .replace("__TOP_LEVEL__", topLevel.toString());
    var document1 =
        new IndexDocument(
            new EventConsumptionAttributes(indexName, SortableIdentifier.next()),
            JsonUtils.dtoObjectMapper.readTree(json1));
    indexingClient.addDocumentToIndex(document1);

    var subunit2 = randomString();
    var json2 =
        """
                    {
          "type": "PublishingRequest",
          "organization": {
            "id": "__SUBUNIT__",
            "identifier": "__SUBUNIT__",
            "partOf": [
              {
                "id": "__TOP_LEVEL__",
                "identifier": "__TOP_LEVEL__"
              }
            ]
          }
        }
        """
            .replace("__SUBUNIT__", subunit2)
            .replace("__TOP_LEVEL__", topLevel);

    var document2 =
        new IndexDocument(
            new EventConsumptionAttributes(indexName, SortableIdentifier.next()),
            JsonUtils.dtoObjectMapper.readTree(json2));
    indexingClient.addDocumentToIndex(document2);
    Thread.sleep(DELAY_AFTER_INDEXING);

    var requestInfo = requestInfoViewingScope(randomUri(), Set.of(subunit1));

    var response =
        TicketSearchQuery.builder()
            .withDockerHostUri(URI.create(container.getHttpHostAddress()))
            .fromRequestInfo(requestInfo)
            .withRequiredParameters(FROM, SIZE, AGGREGATION)
            .build()
            .withFilter()
            .fromRequestInfo(requestInfo)
            .doSearch(searchClient, Words.RESOURCES)
            .toPagedResponse();

    assertEquals(1, response.hits().size());
    assertTrue(response.hits().getFirst().toString().contains(subunit1));
  }

  @Test
  void shouldReturnOnlyTicketsWithViewingsWhenViewingScopeIsProvided()
      throws BadRequestException, UnauthorizedException, IOException, InterruptedException {
    indexingClient.createIndex(indexName, TICKET_MAPPINGS.asMap());

    var viewingScope = randomString();
    var toplevel = randomUri();

    var json1 =
        """
                    {
          "type": "PublishingRequest",
          "organization": {
            "id": "__SUBUNIT__",
            "identifier": "__SUBUNIT__",
            "partOf": [
              {
                "id": "__TOP_LEVEL__",
                "identifier": "__TOP_LEVEL__"
              },
              {
                "id": "__UNIT_FROM_VIEWING_SCOPE__",
                "identifier": "__UNIT_FROM_VIEWING_SCOPE__"
              }
            ]
          }
        }
        """
            .replace("__SUBUNIT__", randomString())
            .replace("__TOP_LEVEL__", toplevel.toString())
            .replace("__UNIT_FROM_VIEWING_SCOPE__", viewingScope);
    var document1 =
        new IndexDocument(
            new EventConsumptionAttributes(indexName, SortableIdentifier.next()),
            JsonUtils.dtoObjectMapper.readTree(json1));
    indexingClient.addDocumentToIndex(document1);

    var json2 =
        """
                    {
          "type": "PublishingRequest",
          "organization": {
            "id": "__SUBUNIT__",
            "identifier": "__SUBUNIT__",
            "partOf": [
              {
                "id": "__TOP_LEVEL__",
                "identifier": "__TOP_LEVEL__"
              }
            ]
          }
        }
        """
            .replace("__SUBUNIT__", randomString())
            .replace("__TOP_LEVEL__", toplevel.toString());
    var document2 =
        new IndexDocument(
            new EventConsumptionAttributes(indexName, SortableIdentifier.next()),
            JsonUtils.dtoObjectMapper.readTree(json2));
    indexingClient.addDocumentToIndex(document2);

    Thread.sleep(DELAY_AFTER_INDEXING);

    var requestInfo = requestInfoViewingScope(toplevel, Set.of(viewingScope));

    var response =
        TicketSearchQuery.builder()
            .withDockerHostUri(URI.create(container.getHttpHostAddress()))
            .fromRequestInfo(requestInfo)
            .withRequiredParameters(FROM, SIZE, AGGREGATION)
            .build()
            .withFilter()
            .fromRequestInfo(requestInfo)
            .doSearch(searchClient, Words.RESOURCES)
            .toPagedResponse();

    assertEquals(1, response.hits().size());
    assertTrue(response.hits().getFirst().toString().contains(viewingScope));
  }

  @Test
  void shouldFilterOnQueryParamWithinViewingScopeWhenViewingScopeHasMultipleUnits()
      throws BadRequestException, UnauthorizedException, IOException, InterruptedException {
    indexingClient.createIndex(indexName, TICKET_MAPPINGS.asMap());

    var viewingScope1 = randomString();
    var toplevel = randomUri();
    var viewingScope2 = randomString();

    var json1 =
        """
                    {
          "type": "PublishingRequest",
          "organization": {
            "id": "__SUBUNIT__",
            "identifier": "__SUBUNIT__",
            "partOf": [
              {
                "id": "__TOP_LEVEL__",
                "identifier": "__TOP_LEVEL__"
              },
              {
                "id": "__UNIT_FROM_VIEWING_SCOPE__",
                "identifier": "__UNIT_FROM_VIEWING_SCOPE__"
              }
            ]
          }
        }
        """
            .replace("__SUBUNIT__", randomString())
            .replace("__TOP_LEVEL__", toplevel.toString())
            .replace("__UNIT_FROM_VIEWING_SCOPE__", viewingScope1);
    var document1 =
        new IndexDocument(
            new EventConsumptionAttributes(indexName, SortableIdentifier.next()),
            JsonUtils.dtoObjectMapper.readTree(json1));
    indexingClient.addDocumentToIndex(document1);

    var json2 =
        """
                    {
          "type": "PublishingRequest",
          "organization": {
            "id": "__SUBUNIT__",
            "identifier": "__SUBUNIT__",
            "partOf": [
              {
                "id": "__TOP_LEVEL__",
                "identifier": "__TOP_LEVEL__"
              },
              {
                "id": "__UNIT_FROM_VIEWING_SCOPE__",
                "identifier": "__UNIT_FROM_VIEWING_SCOPE__"
              }
            ]
          }
        }
        """
            .replace("__SUBUNIT__", randomString())
            .replace("__TOP_LEVEL__", toplevel.toString())
            .replace("__UNIT_FROM_VIEWING_SCOPE__", viewingScope2);
    var document2 =
        new IndexDocument(
            new EventConsumptionAttributes(indexName, SortableIdentifier.next()),
            JsonUtils.dtoObjectMapper.readTree(json2));
    indexingClient.addDocumentToIndex(document2);

    Thread.sleep(DELAY_AFTER_INDEXING);

    var requestInfo =
        requestInfoWithQueryAndViewingScope(
            toplevel,
            Map.of("organizationId", List.of(viewingScope1)),
            Set.of(viewingScope1, viewingScope2));

    var response =
        TicketSearchQuery.builder()
            .withDockerHostUri(URI.create(container.getHttpHostAddress()))
            .fromRequestInfo(requestInfo)
            .withRequiredParameters(FROM, SIZE, AGGREGATION)
            .build()
            .withFilter()
            .fromRequestInfo(requestInfo)
            .doSearch(searchClient, Words.RESOURCES)
            .toPagedResponse();

    assertEquals(1, response.hits().size());
    assertTrue(response.hits().getFirst().toString().contains(viewingScope1.toString()));
  }

  @Test
  void shouldNotReturnDocumentWithTheSameTopLevelThatIsNotWithinViewingScope()
      throws BadRequestException, UnauthorizedException, IOException, InterruptedException {
    indexingClient.createIndex(indexName, TICKET_MAPPINGS.asMap());

    var viewingScope = randomString();
    var toplevel = randomUri();

    var json1 =
        """
                    {
          "type": "PublishingRequest",
          "organization": {
            "id": "__TOP_LEVEL__",
            "identifier": "__TOP_LEVEL__",
            "partOf": [
              {
                "id": "__TOP_LEVEL__",
                "identifier": "__TOP_LEVEL__"
              }
            ]
          }
        }
        """
            .replace("__TOP_LEVEL__", toplevel.toString());
    var document =
        new IndexDocument(
            new EventConsumptionAttributes(indexName, SortableIdentifier.next()),
            JsonUtils.dtoObjectMapper.readTree(json1));
    indexingClient.addDocumentToIndex(document);

    Thread.sleep(DELAY_AFTER_INDEXING);

    var requestInfo = requestInfoViewingScope(toplevel, Set.of(viewingScope));

    var response =
        TicketSearchQuery.builder()
            .withDockerHostUri(URI.create(container.getHttpHostAddress()))
            .fromRequestInfo(requestInfo)
            .withRequiredParameters(FROM, SIZE, AGGREGATION)
            .build()
            .withFilter()
            .fromRequestInfo(requestInfo)
            .doSearch(searchClient, Words.RESOURCES)
            .toPagedResponse();

    assertEquals(0, response.hits().size());
  }

  @Test
  void
      shouldNotReturnDocumentWithTheSameTopLevelThatIsNotWithinViewingScopeWhenConsumingQueryParam()
          throws BadRequestException, UnauthorizedException, IOException, InterruptedException {
    indexingClient.createIndex(indexName, TICKET_MAPPINGS.asMap());

    var viewingScope = randomString();
    var toplevel = randomUri();

    var json1 =
        """
                    {
          "type": "PublishingRequest",
          "organization": {
            "id": "__TOP_LEVEL__",
            "identifier": "__TOP_LEVEL__",
            "partOf": [
              {
                "id": "__TOP_LEVEL__",
                "identifier": "__TOP_LEVEL__"
              }
            ]
          }
        }
        """
            .replace("__TOP_LEVEL__", toplevel.toString());
    var document =
        new IndexDocument(
            new EventConsumptionAttributes(indexName, SortableIdentifier.next()),
            JsonUtils.dtoObjectMapper.readTree(json1));
    indexingClient.addDocumentToIndex(document);

    Thread.sleep(DELAY_AFTER_INDEXING);

    var requestInfo =
        requestInfoWithQueryAndViewingScope(
            toplevel, Map.of("organizationId", List.of(toplevel.toString())), Set.of(viewingScope));

    var response =
        TicketSearchQuery.builder()
            .withDockerHostUri(URI.create(container.getHttpHostAddress()))
            .fromRequestInfo(requestInfo)
            .withRequiredParameters(FROM, SIZE, AGGREGATION)
            .build()
            .withFilter()
            .fromRequestInfo(requestInfo)
            .doSearch(searchClient, Words.RESOURCES)
            .toPagedResponse();

    assertEquals(0, response.hits().size());
  }

  private static RequestInfo requestInfoWithQueryParams(
      URI topLevelOrg, Map<String, List<String>> queryParams) throws UnauthorizedException {
    var requestInfo = mock(RequestInfo.class);
    when(requestInfo.getUserName()).thenReturn(randomString());
    when(requestInfo.getTopLevelOrgCristinId()).thenReturn(Optional.of(topLevelOrg));
    when(requestInfo.getAccessRights())
        .thenReturn(List.of(MANAGE_DOI, MANAGE_PUBLISHING_REQUESTS, SUPPORT));
    when(requestInfo.getHeaders()).thenReturn(Map.of("Authorization", randomString()));
    when(requestInfo.getMultiValueQueryStringParameters()).thenReturn(queryParams);
    return requestInfo;
  }

  private static RequestInfo requestInfoTopLevelOrg(URI topLevelOrgIdentifier)
      throws UnauthorizedException {
    var requestInfo = mock(RequestInfo.class);
    when(requestInfo.getUserName()).thenReturn(randomString());
    when(requestInfo.getTopLevelOrgCristinId()).thenReturn(Optional.of(topLevelOrgIdentifier));
    when(requestInfo.getAccessRights())
        .thenReturn(List.of(MANAGE_DOI, MANAGE_PUBLISHING_REQUESTS, SUPPORT));
    when(requestInfo.getHeaders()).thenReturn(Map.of("Authorization", randomString()));
    return requestInfo;
  }

  private static RequestInfo requestInfoViewingScope(
      URI topLevelOrgIdentifier, Set<String> viewingScopes) throws UnauthorizedException {
    var requestInfo = mock(RequestInfo.class);
    when(requestInfo.getUserName()).thenReturn(randomString());
    when(requestInfo.getTopLevelOrgCristinId()).thenReturn(Optional.of(topLevelOrgIdentifier));
    when(requestInfo.getAccessRights())
        .thenReturn(List.of(MANAGE_DOI, MANAGE_PUBLISHING_REQUESTS, SUPPORT));
    when(requestInfo.getHeaders()).thenReturn(Map.of("Authorization", randomString()));
    when(requestInfo.getViewingScope()).thenReturn(new ViewingScope(viewingScopes, Set.of()));

    return requestInfo;
  }

  private static RequestInfo requestInfoWithQueryAndViewingScope(
      URI topLevelOrgIdentifier,
      Map<String, List<String>> queryParams,
      Set<String> viewingScopeIncludes)
      throws UnauthorizedException {
    var requestInfo = mock(RequestInfo.class);
    when(requestInfo.getUserName()).thenReturn(randomString());
    when(requestInfo.getTopLevelOrgCristinId()).thenReturn(Optional.of(topLevelOrgIdentifier));
    when(requestInfo.getAccessRights())
        .thenReturn(List.of(MANAGE_DOI, MANAGE_PUBLISHING_REQUESTS, SUPPORT));
    when(requestInfo.getHeaders()).thenReturn(Map.of("Authorization", randomString()));

    when(requestInfo.getMultiValueQueryStringParameters()).thenReturn(queryParams);
    when(requestInfo.getViewingScope())
        .thenReturn(new ViewingScope(viewingScopeIncludes, Set.of()));

    return requestInfo;
  }
}
