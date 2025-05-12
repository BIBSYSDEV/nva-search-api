package no.unit.nva.search;

import static no.unit.nva.constants.IndexMappingsAndSettings.RESOURCE_MAPPINGS;
import static no.unit.nva.constants.IndexMappingsAndSettings.RESOURCE_SETTINGS;
import static no.unit.nva.indexing.testutils.MockedJwtProvider.setupMockedCachedJwtProvider;
import static no.unit.nva.search.common.enums.PublicationStatus.PUBLISHED;
import static no.unit.nva.search.common.enums.PublicationStatus.PUBLISHED_METADATA;
import static no.unit.nva.search.resource.ResourceParameter.AGGREGATION;
import static no.unit.nva.search.resource.ResourceParameter.FROM;
import static no.unit.nva.search.resource.ResourceParameter.SIZE;
import static no.unit.nva.search.resource.ResourceParameter.SORT;
import static nva.commons.core.attempt.Try.attempt;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.mock;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.nio.file.Path;
import java.util.stream.Stream;
import no.unit.nva.commons.json.JsonUtils;
import no.unit.nva.constants.Words;
import no.unit.nva.identifiers.SortableIdentifier;
import no.unit.nva.indexingclient.IndexingClient;
import no.unit.nva.indexingclient.models.EventConsumptionAttributes;
import no.unit.nva.indexingclient.models.IndexDocument;
import no.unit.nva.indexingclient.models.RestHighLevelClientWrapper;
import no.unit.nva.search.resource.ResourceClient;
import no.unit.nva.search.resource.ResourceSearchQuery;
import no.unit.nva.search.resource.UserSettingsClient;
import nva.commons.apigateway.RequestInfo;
import nva.commons.apigateway.exceptions.ApiIoException;
import nva.commons.apigateway.exceptions.BadRequestException;
import nva.commons.core.ioutils.IoUtils;
import org.apache.http.HttpHost;
import org.hamcrest.collection.IsIterableContainingInOrder;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.opensearch.client.RestClient;
import org.opensearch.testcontainers.OpensearchContainer;
import org.testcontainers.junit.jupiter.Testcontainers;

@Testcontainers
public class ResourceSearchContainerTests {

  private static final String OPEN_SEARCH_IMAGE = "opensearchproject/opensearch:2.11.0";
  private static final OpensearchContainer container = new OpensearchContainer(OPEN_SEARCH_IMAGE);
  private static final String INDEX_NAME = "resources";
  private static final String INDEX_DOCUMENT_TEMPLATE =
      IoUtils.stringFromResources(Path.of("indexDocumentTemplate.json"));
  private static final String REQUEST_INFO_JSON_TEMPLATE =
      IoUtils.stringFromResources(Path.of("requestInfoTemplate.json"));
  private static final String TITLE_UPPERCASE_A = "A";
  private static final String TITLE_LOWERCASE_A = "A";
  private static final String TITLE_LOWERCASE_B = "b";

  private static IndexingClient indexingClient;
  private static ResourceClient resourceClient;

  @BeforeAll
  public static void beforeAll() {
    container.withEnv("indices.query.bool.max_clause_count", "2048").start();

    var restClientBuilder = RestClient.builder(HttpHost.create(container.getHttpHostAddress()));
    var restHighLevelClientWrapper = new RestHighLevelClientWrapper(restClientBuilder);
    var cachedJwtProvider = setupMockedCachedJwtProvider();
    indexingClient = new IndexingClient(restHighLevelClientWrapper, cachedJwtProvider);

    var userSettingsClient = mock(UserSettingsClient.class);
    resourceClient =
        new ResourceClient(HttpClient.newHttpClient(), cachedJwtProvider, userSettingsClient);
  }

  @AfterAll
  public static void afterAll() {
    container.stop();
  }

  @BeforeEach
  void beforeEach() throws IOException {
    indexingClient.createIndex(INDEX_NAME, RESOURCE_MAPPINGS.asMap(), RESOURCE_SETTINGS.asMap());
  }

  @AfterEach
  public void afterEach() throws IOException {
    indexingClient.deleteIndex(INDEX_NAME);
  }

  @ParameterizedTest
  @MethodSource("sortOrderDataProvider")
  void sortingByTitleIsCaseInsensitive(String sortOrder, String[] expectedOrderOfTitles)
      throws IOException, BadRequestException, ApiIoException {

    populateAndRefreshIndex();

    var requestInfo = getRequestInfo(sortOrder);
    var result =
        ResourceSearchQuery.builder()
            .fromRequestInfo(requestInfo)
            .withDockerHostUri(URI.create(container.getHttpHostAddress()))
            .withRequiredParameters(FROM, SIZE, AGGREGATION, SORT)
            .validate()
            .build()
            .withFilter()
            .requiredStatus(PUBLISHED, PUBLISHED_METADATA)
            .apply()
            .doSearch(resourceClient, Words.RESOURCES);

    var actualOrderOfTitles =
        result.toPagedResponse().hits().stream()
            .map(node -> node.at("/entityDescription/mainTitle").textValue())
            .toList();

    assertThat(actualOrderOfTitles, IsIterableContainingInOrder.contains(expectedOrderOfTitles));
  }

  private void populateAndRefreshIndex() throws IOException {
    indexingClient.addDocumentToIndex(indexDocument(SortableIdentifier.next(), TITLE_UPPERCASE_A));
    indexingClient.addDocumentToIndex(indexDocument(SortableIdentifier.next(), TITLE_LOWERCASE_B));
    indexingClient.addDocumentToIndex(indexDocument(SortableIdentifier.next(), TITLE_LOWERCASE_A));

    indexingClient.refreshIndex(INDEX_NAME);
  }

  static Stream<Arguments> sortOrderDataProvider() {
    return Stream.of(
        Arguments.argumentSet(
            "sort order ascending",
            "asc",
            new String[] {TITLE_LOWERCASE_A, TITLE_UPPERCASE_A, TITLE_LOWERCASE_B}),
        Arguments.argumentSet(
            "sort order descending",
            "desc",
            new String[] {TITLE_LOWERCASE_B, TITLE_LOWERCASE_A, TITLE_UPPERCASE_A}));
  }

  private static RequestInfo getRequestInfo(String sortOrder) throws ApiIoException {
    var actualJson = REQUEST_INFO_JSON_TEMPLATE.replaceAll("@@SORT_ORDER@@", sortOrder);
    return RequestInfo.fromString(actualJson);
  }

  private IndexDocument indexDocument(SortableIdentifier identifier, String title) {
    var document =
        INDEX_DOCUMENT_TEMPLATE
            .replace("@@IDENTIFIER@@", identifier.toString())
            .replace("@@TITLE@@", title);

    return new IndexDocument(
        new EventConsumptionAttributes(INDEX_NAME, identifier),
        attempt(() -> JsonUtils.dtoObjectMapper.readTree(document)).orElseThrow());
  }
}
