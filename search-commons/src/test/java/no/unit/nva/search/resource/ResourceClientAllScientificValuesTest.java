package no.unit.nva.search.resource;

import static no.unit.nva.common.Containers.container;
import static no.unit.nva.common.Containers.indexingClient;
import static no.unit.nva.common.EntrySetTools.queryToMapEntries;
import static no.unit.nva.common.TestConstants.DELAY_AFTER_INDEXING;
import static no.unit.nva.constants.IndexMappingsAndSettings.RESOURCE_MAPPINGS;
import static no.unit.nva.constants.IndexMappingsAndSettings.RESOURCE_SETTINGS;
import static no.unit.nva.indexing.testutils.MockedJwtProvider.setupMockedCachedJwtProvider;
import static no.unit.nva.search.resource.ResourceParameter.FROM;
import static no.unit.nva.search.resource.ResourceParameter.SIZE;
import static nva.commons.core.attempt.Try.attempt;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import no.unit.nva.commons.json.JsonUtils;
import no.unit.nva.identifiers.SortableIdentifier;
import no.unit.nva.indexingclient.models.EventConsumptionAttributes;
import no.unit.nva.indexingclient.models.IndexDocument;
import nva.commons.apigateway.exceptions.BadRequestException;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.testcontainers.junit.jupiter.Testcontainers;

@Testcontainers
public class ResourceClientAllScientificValuesTest {

  private static final String indexName = "resources";
  private static final URI SEARCH_URI = URI.create("https://x.org/?allScientificValues=Unassigned,LevelZero");
  private static ResourceClient resourceClient;

  @BeforeAll
  public static void setUp() throws IOException {
    indexingClient.deleteIndex(indexName);
    var cachedJwtProvider = setupMockedCachedJwtProvider();
    var userSettingsClient = new UserSettingsClient(mock(HttpClient.class), cachedJwtProvider);
    resourceClient = new ResourceClient(HttpClient.newHttpClient(), cachedJwtProvider, userSettingsClient);
  }

  @BeforeEach
  public void cleanIndex() {
    attempt(() -> indexingClient.deleteIndex(indexName));
  }

  @Test
  void shouldReturnDocumentsWhereAllChannelsHaveOneOfProvidedScientificValues()
      throws IOException, InterruptedException, BadRequestException {

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

    var response =
        ResourceSearchQuery.builder()
            .fromTestQueryParameters(queryToMapEntries(SEARCH_URI))
            .withDockerHostUri(URI.create(container.getHttpHostAddress()))
            .withRequiredParameters(FROM, SIZE)
            .build()
            .withFilter()
            .apply()
            .doSearch(resourceClient);
    var hits = response.toPagedResponse().hits();
    assertFalse(hits.isEmpty());
  }

  @Test
  void shouldNotReturnDocumentsWhereAOneOfScientificValuesIsNotAsProvidedInRequest()
      throws IOException, InterruptedException, BadRequestException {

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

    var response =
        ResourceSearchQuery.builder()
            .fromTestQueryParameters(queryToMapEntries(SEARCH_URI))
            .withDockerHostUri(URI.create(container.getHttpHostAddress()))
            .withRequiredParameters(FROM, SIZE)
            .build()
            .withFilter()
            .apply()
            .doSearch(resourceClient);
    var hits = response.toPagedResponse().hits();
    assertTrue(hits.isEmpty());
  }

  @Test
  void shouldReturnDocumentsWhereOnlyOneScientificValuePresentAndMultipleProvidedInRequest()
      throws IOException, InterruptedException, BadRequestException {
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

    var response =
        ResourceSearchQuery.builder()
            .fromTestQueryParameters(queryToMapEntries(SEARCH_URI))
            .withDockerHostUri(URI.create(container.getHttpHostAddress()))
            .withRequiredParameters(FROM, SIZE)
            .build()
            .withFilter()
            .apply()
            .doSearch(resourceClient);
    var hits = response.toPagedResponse().hits();
    assertFalse(hits.isEmpty());
  }

  @Test
  void
      shouldReturnDocumentsWhereOnlyOneScientificValueInPublicationContextPresentAndMultipleProvidedInRequest()
          throws IOException, InterruptedException, BadRequestException {
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

    var response =
        ResourceSearchQuery.builder()
            .fromTestQueryParameters(queryToMapEntries(SEARCH_URI))
            .withDockerHostUri(URI.create(container.getHttpHostAddress()))
            .withRequiredParameters(FROM, SIZE)
            .build()
            .withFilter()
            .apply()
            .doSearch(resourceClient);
    var hits = response.toPagedResponse().hits();
    assertFalse(hits.isEmpty());
  }

  @Test
  void shouldNotReturnDocumentsWhereScientificValuesAreMissingWhenMultipleProvidedValuesInRequest()
      throws IOException, InterruptedException, BadRequestException {
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

    var response =
        ResourceSearchQuery.builder()
            .fromTestQueryParameters(queryToMapEntries(SEARCH_URI))
            .withDockerHostUri(URI.create(container.getHttpHostAddress()))
            .withRequiredParameters(FROM, SIZE)
            .build()
            .withFilter()
            .apply()
            .doSearch(resourceClient);
    var hits = response.toPagedResponse().hits();
    assertTrue(hits.isEmpty());
  }

  @Test
  void shouldNotReturnDocumentsWhereOnlyOneScientificValueAndIsNotAsOneOfValuesAsProvidedInRequest()
      throws IOException, InterruptedException, BadRequestException {
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

    var response =
        ResourceSearchQuery.builder()
            .fromTestQueryParameters(queryToMapEntries(SEARCH_URI))
            .withDockerHostUri(URI.create(container.getHttpHostAddress()))
            .withRequiredParameters(FROM, SIZE)
            .build()
            .withFilter()
            .apply()
            .doSearch(resourceClient);
    var hits = response.toPagedResponse().hits();
    assertTrue(hits.isEmpty());
  }

  private static void createIndexAndIndexDocument(String json)
      throws IOException, InterruptedException {
    indexingClient.createIndex(indexName, RESOURCE_MAPPINGS.asMap(), RESOURCE_SETTINGS.asMap());
    indexingClient.refreshIndex(indexName);
    var document =
        new IndexDocument(
            new EventConsumptionAttributes(indexName, SortableIdentifier.next()),
            JsonUtils.dtoObjectMapper.readTree(json));
    indexingClient.addDocumentToIndex(document);
    Thread.sleep(DELAY_AFTER_INDEXING);
  }
}
