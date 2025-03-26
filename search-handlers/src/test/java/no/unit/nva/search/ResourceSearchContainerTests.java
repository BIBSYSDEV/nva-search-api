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
import no.unit.nva.commons.json.JsonUtils;
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
import org.junit.jupiter.api.Test;
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

  @Test
  void sortingByTitleIsCaseInsensitive() throws IOException, BadRequestException, ApiIoException {
    var identifier1 = SortableIdentifier.next();
    var title1 = "A";
    indexingClient.addDocumentToIndex(indexDocument(identifier1, title1));
    var identifier2 = SortableIdentifier.next();
    var title2 = "b";
    indexingClient.addDocumentToIndex(indexDocument(identifier2, title2));
    var identifier3 = SortableIdentifier.next();
    var title3 = "a";
    indexingClient.addDocumentToIndex(indexDocument(identifier3, title3));

    indexingClient.refreshIndex(INDEX_NAME);

    var requestInfo =
        RequestInfo.fromString(
            IoUtils.stringFromResources(Path.of("requestInfo.json")), HttpClient.newHttpClient());
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
            .doSearch(resourceClient);

    var expectedOrderOfTitles = new String[] {title3, title1, title2};
    var actualOrderOfTitles =
        result.toPagedResponse().hits().stream()
            .map(node -> node.at("/entityDescription/mainTitle").textValue())
            .toList();

    assertThat(actualOrderOfTitles, IsIterableContainingInOrder.contains(expectedOrderOfTitles));
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
