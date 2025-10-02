package no.unit.nva.indexingclient;

import static no.unit.nva.common.Containers.container;
import static no.unit.nva.common.Containers.indexingClient;
import static no.unit.nva.common.EntrySetTools.queryToMapEntries;
import static no.unit.nva.common.MockedHttpResponse.mockedFutureFailed;
import static no.unit.nva.common.MockedHttpResponse.mockedFutureHttpResponse;
import static no.unit.nva.constants.IndexMappingsAndSettings.RESOURCE_MAPPINGS;
import static no.unit.nva.constants.IndexMappingsAndSettings.RESOURCE_SETTINGS;
import static no.unit.nva.indexing.testutils.MockedJwtProvider.setupMockedCachedJwtProvider;
import static no.unit.nva.search.resource.ResourceParameter.FROM;
import static no.unit.nva.search.resource.ResourceParameter.SIZE;
import static no.unit.nva.testutils.RandomDataGenerator.randomString;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.nio.file.Path;
import java.util.Locale;
import no.unit.nva.commons.json.JsonUtils;
import no.unit.nva.identifiers.SortableIdentifier;
import no.unit.nva.indexingclient.models.EventConsumptionAttributes;
import no.unit.nva.indexingclient.models.IndexDocument;
import no.unit.nva.search.common.records.HttpResponseFormatter;
import no.unit.nva.search.resource.ResourceClient;
import no.unit.nva.search.resource.ResourceParameter;
import no.unit.nva.search.resource.ResourceSearchQuery;
import no.unit.nva.search.resource.UserSettingsClient;
import nva.commons.apigateway.exceptions.BadRequestException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.testcontainers.junit.jupiter.Testcontainers;

@Testcontainers
public class TestContainerIndexingClientTest {

  public static final String USER_SETTINGS_JSON = "user_settings.json";
  private static final String indexName = randomString().toLowerCase(Locale.ROOT);
  private static ResourceClient resourceClient;

  @BeforeAll
  public static void setUp() {
    var cachedJwtProvider = setupMockedCachedJwtProvider();
    var mochedHttpClient = mock(HttpClient.class);
    var userSettingsClient = new UserSettingsClient(mochedHttpClient);
    var response = mockedFutureHttpResponse(Path.of(USER_SETTINGS_JSON));
    when(mochedHttpClient.sendAsync(any(), any()))
        .thenReturn(response)
        .thenReturn(mockedFutureHttpResponse(""))
        .thenReturn(mockedFutureFailed());
    resourceClient =
        new ResourceClient(HttpClient.newHttpClient(), cachedJwtProvider, userSettingsClient);
  }

  @AfterEach
  public void cleanIndex() throws IOException {
    indexingClient.deleteIndex(indexName);
  }

  @Test
  void
      shouldUpdateExistingDocumentInSearchIndexWhenCalculatedRoutingKeyDifferFromAlreadyIndexedVersionOfDocument()
          throws IOException, BadRequestException {
    indexingClient.createIndex(indexName, RESOURCE_MAPPINGS.asMap(), RESOURCE_SETTINGS.asMap());
    indexingClient.refreshIndex(indexName);

    var documentIdentifier = "0199849dcafa-ff016f20-86c8-4029-97ae-b016e0a24465";
    var existingDocument =
        """
        {
          "type": "Publication",
          "identifier": "%s",
          "joinField": {
            "name": "partOf",
            "parent": "PARENT_IDENTIFIER_NOT_FOUND"
          }
        }
        """
            .formatted(documentIdentifier);
    var updatedDocument =
        """
        {
          "type": "Publication",
          "identifier": "%s",
          "joinField": {
            "name": "partOf",
            "parent": "019965d29f09-61ec9259-60ce-41fc-a0eb-ebd55e6cec40"
          }
        }
        """
            .formatted(documentIdentifier);
    indexDocument(existingDocument, documentIdentifier);
    indexDocument(updatedDocument, documentIdentifier);

    var response =
        doSearchWithUri(URI.create("https://x.org/?id=%s".formatted(documentIdentifier)));
    var hits = response.toPagedResponse().hits();

    assertEquals(1, hits.size());
  }

  private static void indexDocument(String json, String documentIdentifier) throws IOException {
    var document =
        new IndexDocument(
            new EventConsumptionAttributes(indexName, new SortableIdentifier(documentIdentifier)),
            JsonUtils.dtoObjectMapper.readTree(json));
    indexingClient.addDocumentToIndex(document);
    indexingClient.refreshIndex(indexName);
  }

  private static HttpResponseFormatter<ResourceParameter> doSearchWithUri(URI searchUri)
      throws BadRequestException {
    return ResourceSearchQuery.builder()
        .fromTestQueryParameters(queryToMapEntries(searchUri))
        .withDockerHostUri(URI.create(container.getHttpHostAddress()))
        .withRequiredParameters(FROM, SIZE)
        .build()
        .withFilter()
        .apply()
        .doSearch(resourceClient, indexName);
  }
}
