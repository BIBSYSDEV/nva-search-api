package no.unit.nva.search.ticket;

import static no.unit.nva.common.Containers.container;
import static no.unit.nva.common.Containers.indexingClient;
import static no.unit.nva.common.TestConstants.DELAY_AFTER_INDEXING;
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
import static nva.commons.core.ioutils.IoUtils.stringFromResources;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import no.unit.nva.commons.json.JsonUtils;
import no.unit.nva.identifiers.SortableIdentifier;
import no.unit.nva.indexingclient.models.EventConsumptionAttributes;
import no.unit.nva.indexingclient.models.IndexDocument;
import nva.commons.apigateway.RequestInfo;
import nva.commons.apigateway.exceptions.BadRequestException;
import nva.commons.apigateway.exceptions.UnauthorizedException;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.testcontainers.junit.jupiter.Testcontainers;

@Testcontainers
public class TicketClientViewingScopeTest {

  private static final String TICKET_MAPPING_DEV_JSON = "ticket_mappings_dev.json";
  private static final String indexName = "tickets";
  private static TicketClient searchClient;

  @BeforeAll
  public static void setUp() {
    var cachedJwtProvider = setupMockedCachedJwtProvider();
    searchClient = new TicketClient(HttpClient.newHttpClient(), cachedJwtProvider);
  }

  public static Map<String, Object> loadMapFromResource(String resource) {
    var mappingsJson = stringFromResources(Path.of(resource));
    var type = new TypeReference<Map<String, Object>>() {};
    return attempt(() -> JsonUtils.dtoObjectMapper.readValue(mappingsJson, type)).orElseThrow();
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
    indexingClient.createIndex(indexName, loadMapFromResource(TICKET_MAPPING_DEV_JSON));
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
            .doSearch(searchClient)
            .toPagedResponse();

    assertEquals(1, response.hits().size());
  }

  @Test
  void
      shouldReturnTicketsWhenQueryParamContainsSubUnitOfTopLevelAndTicketOrganizationItThatSubunit()
          throws BadRequestException, UnauthorizedException, IOException, InterruptedException {
    var topLevelOrg = randomUri();
    var subunit = randomUri();

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
            .replace("__SUBUNIT__", subunit.toString())
            .replace("__TOP_LEVEL__", topLevelOrg.toString());
    var document =
        new IndexDocument(
            new EventConsumptionAttributes(indexName, SortableIdentifier.next()),
            JsonUtils.dtoObjectMapper.readTree(json));
    indexingClient.createIndex(indexName, loadMapFromResource(TICKET_MAPPING_DEV_JSON));
    indexingClient.addDocumentToIndex(document);
    Thread.sleep(DELAY_AFTER_INDEXING);

    var requestInfo =
        requestInfoWithQueryParams(
            topLevelOrg, Map.of("organizationId", List.of(subunit.toString())));

    var response =
        TicketSearchQuery.builder()
            .withDockerHostUri(URI.create(container.getHttpHostAddress()))
            .fromRequestInfo(requestInfo)
            .withRequiredParameters(FROM, SIZE, AGGREGATION)
            .build()
            .withFilter()
            .fromRequestInfo(requestInfo)
            .doSearch(searchClient)
            .toPagedResponse();

    assertEquals(1, response.hits().size());
  }

  @Test
  void shouldNotReturnTicketsWhenOrganizationsNotMatch()
      throws BadRequestException, UnauthorizedException, IOException, InterruptedException {

    var subunit = randomUri();
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
            .replace("__SUBUNIT__", subunit.toString())
            .replace("__TOP_LEVEL__", randomUri().toString());
    var document =
        new IndexDocument(
            new EventConsumptionAttributes(indexName, SortableIdentifier.next()),
            JsonUtils.dtoObjectMapper.readTree(json));
    indexingClient.createIndex(indexName, loadMapFromResource(TICKET_MAPPING_DEV_JSON));
    indexingClient.addDocumentToIndex(document);
    Thread.sleep(DELAY_AFTER_INDEXING);

    var requestInfo =
        requestInfoWithQueryParams(
            randomUri(), Map.of("organizationId", List.of(subunit.toString())));

    var response =
        TicketSearchQuery.builder()
            .withDockerHostUri(URI.create(container.getHttpHostAddress()))
            .fromRequestInfo(requestInfo)
            .withRequiredParameters(FROM, SIZE, AGGREGATION)
            .build()
            .withFilter()
            .fromRequestInfo(requestInfo)
            .doSearch(searchClient)
            .toPagedResponse();

    assertEquals(0, response.hits().size());
  }

  @Test
  void shouldReturnOnlyTicketsWithSubunitFromQueryParamsWhenProvided()
      throws BadRequestException, UnauthorizedException, IOException, InterruptedException {
    indexingClient.createIndex(indexName, loadMapFromResource(TICKET_MAPPING_DEV_JSON));

    var subunit1 = randomUri();
    var topLevel = randomUri();
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

    var subunit2 = randomUri();
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
            .replace("__SUBUNIT__", subunit2.toString())
            .replace("__TOP_LEVEL__", topLevel.toString());

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
            .doSearch(searchClient)
            .toPagedResponse();

    assertEquals(1, response.hits().size());
    assertTrue(response.hits().getFirst().toString().contains(subunit1.toString()));
  }

  @Test
  void shouldReturnOnlyTicketsWithViewingsWhenViewingScopeIsProvided()
      throws BadRequestException, UnauthorizedException, IOException, InterruptedException {
    indexingClient.createIndex(indexName, loadMapFromResource(TICKET_MAPPING_DEV_JSON));

    var viewingScope = randomUri();
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
            .replace("__UNIT_FROM_VIEWING_SCOPE__", viewingScope.toString());
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
            .doSearch(searchClient)
            .toPagedResponse();

    assertEquals(1, response.hits().size());
    assertTrue(response.hits().getFirst().toString().contains(viewingScope.toString()));
  }

  @Test
  void shouldFilterOnQueryParamWithinViewingScopeWhenViewingScopeHasMultipleUnits()
      throws BadRequestException, UnauthorizedException, IOException, InterruptedException {
    indexingClient.createIndex(indexName, loadMapFromResource(TICKET_MAPPING_DEV_JSON));

    var viewingScope1 = randomUri();
    var toplevel = randomUri();
    var viewingScope2 = randomUri();

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
            .replace("__UNIT_FROM_VIEWING_SCOPE__", viewingScope1.toString());
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
            .replace("__UNIT_FROM_VIEWING_SCOPE__", viewingScope2.toString());
    var document2 =
        new IndexDocument(
            new EventConsumptionAttributes(indexName, SortableIdentifier.next()),
            JsonUtils.dtoObjectMapper.readTree(json2));
    indexingClient.addDocumentToIndex(document2);

    Thread.sleep(DELAY_AFTER_INDEXING);

    var requestInfo =
        requestInfoWithQueryAndViewingScope(
            toplevel,
            Map.of("organizationId", List.of(viewingScope1.toString())),
            Set.of(viewingScope1, viewingScope2));

    var response =
        TicketSearchQuery.builder()
            .withDockerHostUri(URI.create(container.getHttpHostAddress()))
            .fromRequestInfo(requestInfo)
            .withRequiredParameters(FROM, SIZE, AGGREGATION)
            .build()
            .withFilter()
            .fromRequestInfo(requestInfo)
            .doSearch(searchClient)
            .toPagedResponse();

    assertEquals(1, response.hits().size());
    assertTrue(response.hits().getFirst().toString().contains(viewingScope1.toString()));
  }

  @Test
  void shouldNotReturnDocumentWithTheSameTopLevelThatIsNotWithinViewingScope()
      throws BadRequestException, UnauthorizedException, IOException, InterruptedException {
    indexingClient.createIndex(indexName, loadMapFromResource(TICKET_MAPPING_DEV_JSON));

    var viewingScope = randomUri();
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
            .doSearch(searchClient)
            .toPagedResponse();

    assertEquals(0, response.hits().size());
  }

  @Test
  void
      shouldNotReturnDocumentWithTheSameTopLevelThatIsNotWithinViewingScopeWhenConsumingQueryParam()
          throws BadRequestException, UnauthorizedException, IOException, InterruptedException {
    indexingClient.createIndex(indexName, loadMapFromResource(TICKET_MAPPING_DEV_JSON));

    var viewingScope = randomUri();
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
            .doSearch(searchClient)
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
      URI topLevelOrgIdentifier, Set<URI> viewingScopes)
      throws UnauthorizedException, JsonProcessingException {
    var requestInfo = mock(RequestInfo.class);
    when(requestInfo.getUserName()).thenReturn(randomString());
    when(requestInfo.getTopLevelOrgCristinId()).thenReturn(Optional.of(topLevelOrgIdentifier));
    when(requestInfo.getAccessRights())
        .thenReturn(List.of(MANAGE_DOI, MANAGE_PUBLISHING_REQUESTS, SUPPORT));
    when(requestInfo.getHeaders()).thenReturn(Map.of("Authorization", randomString()));
    when(requestInfo.getRequestContext())
        .thenReturn(
            JsonUtils.dtoObjectMapper.readTree(
                """
                                {
                  "authorizer": {
                    "claims": {
                      "custom:viewingScopeIncluded": "__VIEWING_SCOPE__"
                    }
                  }
                }
                """
                    .replace(
                        "__VIEWING_SCOPE__",
                        viewingScopes.stream()
                            .map(URI::toString)
                            .collect(Collectors.joining(",")))));

    return requestInfo;
  }

  private static RequestInfo requestInfoWithQueryAndViewingScope(
      URI topLevelOrgIdentifier, Map<String, List<String>> queryParams, Set<URI> viewingScopes)
      throws UnauthorizedException, JsonProcessingException {
    var requestInfo = mock(RequestInfo.class);
    when(requestInfo.getUserName()).thenReturn(randomString());
    when(requestInfo.getTopLevelOrgCristinId()).thenReturn(Optional.of(topLevelOrgIdentifier));
    when(requestInfo.getAccessRights())
        .thenReturn(List.of(MANAGE_DOI, MANAGE_PUBLISHING_REQUESTS, SUPPORT));
    when(requestInfo.getHeaders()).thenReturn(Map.of("Authorization", randomString()));

    when(requestInfo.getMultiValueQueryStringParameters()).thenReturn(queryParams);
    when(requestInfo.getRequestContext())
        .thenReturn(
            JsonUtils.dtoObjectMapper.readTree(
                """
                                {
                  "authorizer": {
                    "claims": {
                      "custom:viewingScopeIncluded": "__VIEWING_SCOPE__"
                    }
                  }
                }
                """
                    .replace(
                        "__VIEWING_SCOPE__",
                        viewingScopes.stream()
                            .map(URI::toString)
                            .collect(Collectors.joining(",")))));

    return requestInfo;
  }
}
