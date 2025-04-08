package no.unit.nva.search.resource;

import static no.unit.nva.common.Containers.container;
import static no.unit.nva.common.Containers.indexingClient;
import static no.unit.nva.common.EntrySetTools.queryToMapEntries;
import static no.unit.nva.common.MockedHttpResponse.mockedFutureFailed;
import static no.unit.nva.common.MockedHttpResponse.mockedFutureHttpResponse;
import static no.unit.nva.constants.IndexMappingsAndSettings.RESOURCE_MAPPINGS;
import static no.unit.nva.constants.IndexMappingsAndSettings.RESOURCE_SETTINGS;
import static no.unit.nva.indexing.testutils.MockedJwtProvider.setupMockedCachedJwtProvider;
import static no.unit.nva.search.resource.ResourceClientTest.USER_SETTINGS_JSON;
import static no.unit.nva.search.resource.ResourceParameter.FROM;
import static no.unit.nva.search.resource.ResourceParameter.SIZE;
import static no.unit.nva.testutils.RandomDataGenerator.randomString;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
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
import nva.commons.apigateway.exceptions.BadRequestException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.testcontainers.junit.jupiter.Testcontainers;

@Testcontainers
public class ResourceClientAllScientificValuesTest {

  private static final String indexName = randomString().toLowerCase(Locale.ROOT);
  private static final URI ALL_SCIENTIFIC_VALUES_UNASSIGNED_AND_LEVEL_ZERO =
      URI.create("https://x.org/?allScientificValues=Unassigned,LevelZero");
  private static ResourceClient resourceClient;

  @BeforeAll
  public static void setUp() {
    var cachedJwtProvider = setupMockedCachedJwtProvider();
    var mochedHttpClient = mock(HttpClient.class);
    var userSettingsClient = new UserSettingsClient(mochedHttpClient, cachedJwtProvider);
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
  void shouldReturnDocumentsWhereAllChannelsHaveOneOfProvidedScientificValues()
      throws IOException, BadRequestException {

    var json =
        """
        {
          "type": "Publication",
          "identifier": "018ba3cfcb9c-94f77a1e-ac36-430a-84b0-0619e3bbaf39",
          "entityDescription": {
            "reference": {
              "publicationContext": {
                "publisher": {
                  "type": "Publisher",
                  "scientificValue": "LevelZero"
                },
                "series": {
                  "type": "Series",
                  "scientificValue": "Unassigned"
                }
              }
            }
          }
        }
        """;
    createIndexAndIndexDocument(json);

    var response = doSearchWithUri(ALL_SCIENTIFIC_VALUES_UNASSIGNED_AND_LEVEL_ZERO);
    var hits = response.toPagedResponse().hits();
    assertFalse(hits.isEmpty());
  }

  @Test
  void shouldNotReturnDocumentsWhereAOneOfScientificValuesIsNotAsProvidedInRequest()
      throws IOException, BadRequestException {

    var json =
        """
                {
          "type": "Publication",
          "identifier": "018ba3cfcb9c-94f77a1e-ac36-430a-84b0-0619e3bbaf39",
          "entityDescription": {
            "reference": {
              "publicationContext": {
                "publisher": {
                  "type": "Publisher",
                  "scientificValue": "LevelOne"
                },
                "series": {
                  "type": "Series",
                  "scientificValue": "Unassigned"
                }
              }
            }
          }
        }
        """;
    createIndexAndIndexDocument(json);

    var response = doSearchWithUri(ALL_SCIENTIFIC_VALUES_UNASSIGNED_AND_LEVEL_ZERO);
    var hits = response.toPagedResponse().hits();
    assertTrue(hits.isEmpty());
  }

  @Test
  void shouldReturnDocumentsWhereOnlyOneScientificValuePresentAndMultipleProvidedInRequest()
      throws IOException, BadRequestException {
    var json =
        """
              {
          "type": "Publication",
          "identifier": "018ba3cfcb9c-94f77a1e-ac36-430a-84b0-0619e3bbaf39",
          "entityDescription": {
            "reference": {
              "publicationContext": {
                "publisher": {
                  "type": "Publisher",
                  "scientificValue": "LevelZero"
                }
              }
            }
          }
        }
        """;
    createIndexAndIndexDocument(json);

    var response = doSearchWithUri(ALL_SCIENTIFIC_VALUES_UNASSIGNED_AND_LEVEL_ZERO);
    var hits = response.toPagedResponse().hits();
    assertFalse(hits.isEmpty());
  }

  @Test
  void
      shouldReturnDocumentsWhereOnlyOneScientificValueInPublicationContextPresentAndMultipleProvidedInRequest()
          throws IOException, BadRequestException {
    var json =
        """
              {
          "type": "Publication",
          "identifier": "018ba3cfcb9c-94f77a1e-ac36-430a-84b0-0619e3bbaf39",
          "entityDescription": {
            "reference": {
              "publicationContext": {
                  "type": "Journal",
                  "scientificValue": "LevelZero"
              }
            }
          }
        }
        """;
    createIndexAndIndexDocument(json);

    var response = doSearchWithUri(ALL_SCIENTIFIC_VALUES_UNASSIGNED_AND_LEVEL_ZERO);
    var hits = response.toPagedResponse().hits();
    assertFalse(hits.isEmpty());
  }

  @Test
  void shouldNotReturnDocumentsWhereScientificValuesAreMissingWhenMultipleProvidedValuesInRequest()
      throws IOException, BadRequestException {
    var json =
        """
              {
          "type": "Publication",
          "identifier": "018ba3cfcb9c-94f77a1e-ac36-430a-84b0-0619e3bbaf39",
          "entityDescription": {
            "reference": {

            }
          }
        }
        """;
    createIndexAndIndexDocument(json);

    var response = doSearchWithUri(ALL_SCIENTIFIC_VALUES_UNASSIGNED_AND_LEVEL_ZERO);
    var hits = response.toPagedResponse().hits();
    assertTrue(hits.isEmpty());
  }

  @Test
  void shouldNotReturnDocumentsWhereOnlyOneScientificValueAndIsNotAsOneOfValuesAsProvidedInRequest()
      throws IOException, BadRequestException {
    var json =
        """
                     {
          "type": "Publication",
          "identifier": "018ba3cfcb9c-94f77a1e-ac36-430a-84b0-0619e3bbaf39",
          "entityDescription": {
            "reference": {
              "publicationContext": {
                "publisher": {
                  "type": "Publisher",
                  "scientificValue": "LevelOne"
                }
              }
            }
          }
        }
        """;
    createIndexAndIndexDocument(json);

    var response = doSearchWithUri(ALL_SCIENTIFIC_VALUES_UNASSIGNED_AND_LEVEL_ZERO);
    var hits = response.toPagedResponse().hits();
    assertTrue(hits.isEmpty());
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

  private static void createIndexAndIndexDocument(String json) throws IOException {
    indexingClient.createIndex(indexName, RESOURCE_MAPPINGS.asMap(), RESOURCE_SETTINGS.asMap());
    indexingClient.refreshIndex(indexName);
    var document =
        new IndexDocument(
            new EventConsumptionAttributes(indexName, SortableIdentifier.next()),
            JsonUtils.dtoObjectMapper.readTree(json));
    indexingClient.addDocumentToIndex(document);
    indexingClient.refreshIndex(indexName);
  }
}
