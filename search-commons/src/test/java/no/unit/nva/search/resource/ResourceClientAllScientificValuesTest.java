package no.unit.nva.search.resource;

import static java.lang.Boolean.FALSE;
import static java.lang.Boolean.TRUE;
import static java.util.function.Predicate.not;
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
import static no.unit.nva.testutils.RandomDataGenerator.randomUri;
import static nva.commons.core.StringUtils.EMPTY_STRING;
import static nva.commons.core.attempt.Try.attempt;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import no.unit.nva.commons.json.JsonUtils;
import no.unit.nva.identifiers.SortableIdentifier;
import no.unit.nva.indexingclient.models.EventConsumptionAttributes;
import no.unit.nva.indexingclient.models.IndexDocument;
import no.unit.nva.search.common.records.HttpResponseFormatter;
import nva.commons.apigateway.exceptions.BadRequestException;
import nva.commons.core.paths.UriWrapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
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
  void shouldReturnDocumentsWhereAllChannelsHaveOneOfProvidedScientificValues()
      throws IOException, BadRequestException {

    var json = jsonWithScientificValueForPublisherAndSeries("LevelZero", "Unassigned");

    createIndexAndIndexDocument(json);

    var response = doSearchWithUri(ALL_SCIENTIFIC_VALUES_UNASSIGNED_AND_LEVEL_ZERO);
    var hits = response.toPagedResponse().hits();
    assertFalse(hits.isEmpty());
  }

  @Test
  void shouldNotReturnDocumentsWhereAOneOfScientificValuesIsNotAsProvidedInRequest()
      throws IOException, BadRequestException {

    var json = jsonWithScientificValueForPublisherAndSeries("LevelOne", "Unassigned");
    createIndexAndIndexDocument(json);

    var response = doSearchWithUri(ALL_SCIENTIFIC_VALUES_UNASSIGNED_AND_LEVEL_ZERO);
    var hits = response.toPagedResponse().hits();
    assertTrue(hits.isEmpty());
  }

  @Test
  void shouldReturnDocumentsWhereOnlyOneScientificValuePresentAndMultipleProvidedInRequest()
      throws IOException, BadRequestException {
    var json = jsonWithScientificValueForPublisherAndSeries("LevelZero", null);

    createIndexAndIndexDocument(json);

    var response = doSearchWithUri(ALL_SCIENTIFIC_VALUES_UNASSIGNED_AND_LEVEL_ZERO);
    var hits = response.toPagedResponse().hits();
    assertFalse(hits.isEmpty());
  }

  @Test
  void
      shouldReturnDocumentsWhereOnlyOneScientificValueInPublicationContextPresentAndMultipleProvidedInRequest()
          throws IOException, BadRequestException {
    var json = jsonWithScientificValueForJournal("LevelZero");
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
    var json = jsonWithScientificValueForPublisherAndSeries("LevelOne", null);
    createIndexAndIndexDocument(json);

    var response = doSearchWithUri(ALL_SCIENTIFIC_VALUES_UNASSIGNED_AND_LEVEL_ZERO);
    var hits = response.toPagedResponse().hits();
    assertTrue(hits.isEmpty());
  }

  @Test
  void shouldReturnDocumentWithUnconfirmedJournalWhenSearchingForJournal()
      throws IOException, BadRequestException {
    var journalTitle = randomString();
    var json =
        """
            {
              "type": "Publication",
              "identifier": "0198cc96b890-5221138f-0a8b-47b3-9e18-3826921287ad",
              "entityDescription": {
                "type": "EntityDescription",
                "reference": {
                  "type": "Reference",
                  "publicationContext": {
                    "type": "UnconfirmedJournal",
                    "title": "%s"
                  }
                }
              }
            }
        """
            .formatted(journalTitle);
    createIndexAndIndexDocument(json);

    var response = doSearchWithUri(uriWithQueryParameter("journal", journalTitle));
    assertFalse(response.toPagedResponse().hits().isEmpty());
  }

  @Test
  void shouldReturnDocumentWithUnconfirmedSeriesWhenSearchingForSeries()
      throws IOException, BadRequestException {
    var seriesTitle = randomString();
    var json =
        """
            {
              "type": "Publication",
              "identifier": "0198cc96b890-5221138f-0a8b-47b3-9e18-3826921287ad",
              "entityDescription": {
                "type": "EntityDescription",
                "reference": {
                  "type": "Reference",
                  "publicationContext": {
                    "type": "Report",
                    "series": {
                      "type": "UnconfirmedSeries",
                      "title": "%s"
                    }
                  }
                }
              }
            }
        """
            .formatted(seriesTitle);
    createIndexAndIndexDocument(json);

    var response = doSearchWithUri(uriWithQueryParameter("series", seriesTitle));

    assertFalse(response.toPagedResponse().hits().isEmpty());
  }

  @Test
  void shouldReturnDocumentWithUnconfirmedPublisherWhenSearchingForPublisher()
      throws IOException, BadRequestException {
    var publisherName = randomString();
    var json =
        """
            {
                  "type": "Publication",
                  "identifier": "0198cc96b890-5221138f-0a8b-47b3-9e18-3826921287ad",
                  "entityDescription": {
                    "type": "EntityDescription",
                    "reference": {
                      "type": "Reference",
                      "publicationContext": {
                        "type": "Report",
                        "publisher": {
                          "type": "UnconfirmedPublisher",
                          "name": "%s"
                        }
                      }
                    }
                  }
                }
        """
            .formatted(publisherName);
    createIndexAndIndexDocument(json);

    var response = doSearchWithUri(uriWithQueryParameter("publisher", publisherName));

    assertFalse(response.toPagedResponse().hits().isEmpty());
  }

  @Test
  void shouldReturnDocumentsWithIsbnWhenHasIsbnIsSetToTrue()
      throws BadRequestException, IOException {
    var json =
        """
            {
                  "type": "Publication",
                  "identifier": "0198cc96b890-5221138f-0a8b-47b3-9e18-3826921287ad",
                  "entityDescription": {
                    "type": "EntityDescription",
                    "reference": {
                      "type": "Reference",
                      "publicationContext": {
                        "type": "Report",
                        "isbnList": [ "9781897852323" ]
                      }
                    }
                  }
                }
        """;
    createIndexAndIndexDocument(json);
    var response = doSearchWithUri(uriWithQueryParameter("hasIsbn", TRUE.toString()));

    assertFalse(response.toPagedResponse().hits().isEmpty());
  }

  @Test
  void shouldReturnDocumentWithScientificValueForSeries() throws IOException, BadRequestException {
    var scientificValue = randomString();
    var json = jsonWithScientificValueForPublisherAndSeries(null, scientificValue);
    createIndexAndIndexDocument(json);

    var response = doSearchWithUri(uriWithQueryParameter("scientificValueSeries", scientificValue));

    assertEquals(1, response.toPagedResponse().hits().size());
  }

  @Test
  void
      shouldNotReturnDocumentWithScientificValueForSeriesWhenSeriesIsMissingValueButPublisherHasTheSameScientificValue()
          throws IOException, BadRequestException {
    var scientificValue = randomString();
    var json = jsonWithScientificValueForPublisherAndSeries(scientificValue, null);
    createIndexAndIndexDocument(json);

    var response = doSearchWithUri(uriWithQueryParameter("scientificValueSeries", scientificValue));

    assertTrue(response.toPagedResponse().hits().isEmpty());
  }

  @Test
  void shouldReturnDocumentWithScientificValueForPublisher()
      throws IOException, BadRequestException {
    var scientificValue = randomString();
    var json = jsonWithScientificValueForPublisherAndSeries(scientificValue, null);
    createIndexAndIndexDocument(json);

    var response =
        doSearchWithUri(uriWithQueryParameter("scientificValuePublisher", scientificValue));

    assertEquals(1, response.toPagedResponse().hits().size());
  }

  @Test
  void
      shouldNotReturnDocumentWithScientificValueForPublisherWhenPublisherIsMissingValueButSeriesHasTheSameScientificValue()
          throws IOException, BadRequestException {
    var scientificValue = randomString();
    var json = jsonWithScientificValueForPublisherAndSeries(null, scientificValue);
    createIndexAndIndexDocument(json);

    var response =
        doSearchWithUri(uriWithQueryParameter("scientificValuePublisher", scientificValue));

    assertTrue(response.toPagedResponse().hits().isEmpty());
  }

  @Test
  void shouldNotReturnDocumentWhenExcludedScientificValuePublisherIsProvided()
      throws IOException, BadRequestException {
    var excludedScientificValue = randomString();
    var json = jsonWithScientificValueForPublisherAndSeries(excludedScientificValue, null);
    createIndexAndIndexDocument(json);

    var response =
        doSearchWithUri(
            uriWithQueryParameter("excludedScientificValuePublisher", excludedScientificValue));

    assertTrue(response.toPagedResponse().hits().isEmpty());
  }

  @Test
  void shouldNotReturnDocumentWhenExcludedScientificValueSeriesIsProvided()
      throws IOException, BadRequestException {
    var excludedScientificValue = randomString();
    var json = jsonWithScientificValueForPublisherAndSeries(null, excludedScientificValue);
    createIndexAndIndexDocument(json);

    var response =
        doSearchWithUri(
            uriWithQueryParameter("excludedScientificValueSeries", excludedScientificValue));

    assertTrue(response.toPagedResponse().hits().isEmpty());
  }

  @Test
  void shouldNotReturnDocumentsWhenMultipleExcludedScientificValueSeriesAreProvided()
      throws IOException, BadRequestException {
    var excludedScientificValues = List.of(randomString(), randomString());
    var publicationsWithSeriesWithScientificValues =
        excludedScientificValues.stream()
            .map(value -> jsonWithScientificValueForPublisherAndSeries(null, value))
            .toArray(String[]::new);
    createIndexAndIndexDocument(publicationsWithSeriesWithScientificValues);

    var response =
        doSearchWithUri(
            uriWithQueryParameter(
                "excludedScientificValueSeries", String.join(",", excludedScientificValues)));

    assertTrue(response.toPagedResponse().hits().isEmpty());
  }

  @Test
  void shouldNotReturnDocumentsWhenMultipleExcludedScientificValuePublisherAreProvided()
      throws IOException, BadRequestException {
    var excludedScientificValues = List.of(randomString(), randomString());
    var publicationsWithPublisherWithScientificValues =
        excludedScientificValues.stream()
            .map(
                publisherScientificValue ->
                    jsonWithScientificValueForPublisherAndSeries(publisherScientificValue, null))
            .toArray(String[]::new);
    createIndexAndIndexDocument(publicationsWithPublisherWithScientificValues);

    var response =
        doSearchWithUri(
            uriWithQueryParameter(
                "excludedScientificValuePublisher", String.join(",", excludedScientificValues)));

    assertTrue(response.toPagedResponse().hits().isEmpty());
  }

  @Test
  void shouldReturnDocumentsWithoutIsbnWhenHasIsbnIsSetToFalse()
      throws BadRequestException, IOException {
    var json =
        """
            {
                  "type": "Publication",
                  "identifier": "0198cc96b890-5221138f-0a8b-47b3-9e18-3826921287ad",
                  "entityDescription": {
                    "type": "EntityDescription",
                    "reference": {
                      "type": "Reference",
                      "publicationContext": {
                        "type": "Report",
                        "isbnList": [ ]
                      }
                    }
                  }
                }
        """;
    createIndexAndIndexDocument(json);
    var response = doSearchWithUri(uriWithQueryParameter("hasIsbn", FALSE.toString()));

    assertEquals(1, response.toPagedResponse().hits().size());
  }

  @ParameterizedTest
  @ValueSource(
      strings = {
        """
        { "onlineIssn": "1903-6523" }
        """,
        """
        { "printIssn": "1903-6523" }
        """,
      })
  void shouldReturnDocumentsWithIssnWhenHasIssnIsSetToTrue(String series)
      throws BadRequestException, IOException {
    var json =
        """
            {
                  "type": "Publication",
                  "identifier": "0198cc96b890-5221138f-0a8b-47b3-9e18-3826921287ad",
                  "entityDescription": {
                    "type": "EntityDescription",
                    "reference": {
                      "type": "Reference",
                      "publicationContext": {
                        "type": "Report",
                        "series" : %s
                      }
                    }
                  }
                }
        """
            .formatted(series);
    createIndexAndIndexDocument(json);
    var response = doSearchWithUri(uriWithQueryParameter("hasIssn", TRUE.toString()));

    assertEquals(1, response.toPagedResponse().hits().size());
  }

  @Test
  void shouldReturnDocumentsWithoutIssnWhenHasIssnIsSetToFalse()
      throws BadRequestException, IOException {
    var json =
        """
            {
                  "type": "Publication",
                  "identifier": "0198cc96b890-5221138f-0a8b-47b3-9e18-3826921287ad",
                  "entityDescription": {
                    "type": "EntityDescription",
                    "reference": {
                      "type": "Reference",
                      "publicationContext": {
                        "type": "Report",
                        "series" : {
                        }
                      }
                    }
                  }
                }
        """;
    createIndexAndIndexDocument(json);
    var response = doSearchWithUri(uriWithQueryParameter("hasIssn", FALSE.toString()));

    assertEquals(1, response.toPagedResponse().hits().size());
  }

  private static URI uriWithQueryParameter(String name, String value) {
    return UriWrapper.fromUri(randomUri()).addQueryParameter(name, value).getUri();
  }

  private static String jsonWithScientificValueForJournal(String scientificValue) {
    return """
                 {
             "type": "Publication",
             "identifier": "018ba3cfcb9c-94f77a1e-ac36-430a-84b0-0619e3bbaf39",
             "entityDescription": {
               "reference": {
                 "publicationContext": {
                     "type": "Journal",
                     "scientificValue": "%s"
                 }
               }
             }
           }
           """
        .formatted(scientificValue);
  }

  private static String jsonWithScientificValueForPublisherAndSeries(
      String publisherScientificValue, String seriesScientificValue) {
    var publisherNode = publisherNodeWithScientificValue(publisherScientificValue);
    var seriesNode = seriesNodeWithScientificValue(seriesScientificValue);
    var publicationContext = publicationContextWithPublisherAndSeries(publisherNode, seriesNode);
    return """
           {
             "type": "Publication",
             "identifier": "018ba3cfcb9c-94f77a1e-ac36-430a-84b0-0619e3bbaf39",
             "entityDescription": {
               "reference": {
                 "publicationContext": {
                   %s
                 }
               }
             }
           }
           """
        .formatted(publicationContext);
  }

  private static String publicationContextWithPublisherAndSeries(
      String publisherNode, String seriesNode) {
    return Stream.of(publisherNode, seriesNode)
        .filter(not(String::isEmpty))
        .collect(Collectors.joining(",\n"));
  }

  private static String seriesNodeWithScientificValue(String scientificValue) {
    return Optional.ofNullable(scientificValue)
        .map(ResourceClientAllScientificValuesTest::seriesWithScientificValue)
        .orElse(EMPTY_STRING);
  }

  private static String publisherNodeWithScientificValue(String scientificValue) {
    return Optional.ofNullable(scientificValue)
        .map(ResourceClientAllScientificValuesTest::publisherWithScientificValue)
        .orElse(EMPTY_STRING);
  }

  private static String seriesWithScientificValue(String seriesScientificValue) {
    return """
           "series": {
             "type": "Series",
             "scientificValue": "%s"
           }
           """
        .formatted(seriesScientificValue);
  }

  private static String publisherWithScientificValue(String publisherScientificValue) {
    return """
           "publisher": {
             "type": "Publisher",
             "scientificValue": "%s"
           }
           """
        .formatted(publisherScientificValue);
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

  private static void createIndexAndIndexDocument(String... json) throws IOException {
    indexingClient.createIndex(indexName, RESOURCE_MAPPINGS.asMap(), RESOURCE_SETTINGS.asMap());
    indexingClient.refreshIndex(indexName);
    var documents = Arrays.stream(json).map(ResourceClientAllScientificValuesTest::toIndexDocument);
    documents.forEach(document -> attempt(() -> indexingClient.addDocumentToIndex(document)));
    indexingClient.refreshIndex(indexName);
  }

  private static IndexDocument toIndexDocument(String body) {
    return new IndexDocument(
        new EventConsumptionAttributes(indexName, SortableIdentifier.next()),
        attempt(() -> JsonUtils.dtoObjectMapper.readTree(body)).orElseThrow());
  }
}
