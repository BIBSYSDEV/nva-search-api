package no.unit.nva.common;

import static java.util.Objects.nonNull;
import static no.unit.nva.common.TestConstants.DELAY_AFTER_INDEXING;
import static no.unit.nva.common.TestConstants.OPEN_SEARCH_IMAGE;
import static no.unit.nva.constants.IndexMappingsAndSettings.IMPORT_CANDIDATE_MAPPINGS;
import static no.unit.nva.constants.IndexMappingsAndSettings.RESOURCE_MAPPINGS;
import static no.unit.nva.constants.IndexMappingsAndSettings.RESOURCE_SETTINGS;
import static no.unit.nva.constants.IndexMappingsAndSettings.TICKET_MAPPINGS;
import static no.unit.nva.constants.Words.IDENTIFIER;
import static no.unit.nva.constants.Words.IMPORT_CANDIDATES_INDEX;
import static no.unit.nva.constants.Words.RESOURCES;
import static no.unit.nva.constants.Words.TICKETS;
import static no.unit.nva.indexing.testutils.MockedJwtProvider.setupMockedCachedJwtProvider;
import static nva.commons.core.attempt.Try.attempt;
import static nva.commons.core.ioutils.IoUtils.stringFromResources;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Path;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import no.unit.nva.commons.json.JsonUtils;
import no.unit.nva.identifiers.SortableIdentifier;
import no.unit.nva.indexingclient.IndexingClient;
import no.unit.nva.indexingclient.models.EventConsumptionAttributes;
import no.unit.nva.indexingclient.models.IndexDocument;
import no.unit.nva.indexingclient.models.RestHighLevelClientWrapper;
import org.apache.hc.core5.http.HttpHost;
import org.opensearch.client.RestClient;
import org.opensearch.testcontainers.OpenSearchContainer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.junit.jupiter.Testcontainers;

@Testcontainers
public class Containers {

  public static final OpenSearchContainer<?> container =
      new OpenSearchContainer<>(OPEN_SEARCH_IMAGE);
  private static final Logger logger = LoggerFactory.getLogger(Containers.class);

  private static final String RESOURCE_DATASOURCE_JSON = "resource_datasource.json";

  private static final String TICKET_DATASOURCE_JSON = "ticket_datasource.json";

  private static final String IMPORT_CANDIDATE_DATASOURCE_JSON = "import_candidate_datasource.json";
  public static IndexingClient indexingClient;

  public static void setup() {
    container.withEnv("indices.query.bool.max_clause_count", "2048").start();

    container.waitingFor(Wait.forListeningPort().withStartupTimeout(Duration.ofSeconds(60)));

    try {
      var restClientBuilder = RestClient.builder(HttpHost.create(container.getHttpHostAddress()));
      var restHighLevelClientWrapper = new RestHighLevelClientWrapper(restClientBuilder);
      var cachedJwtProvider = setupMockedCachedJwtProvider();
      indexingClient = new IndexingClient(restHighLevelClientWrapper, cachedJwtProvider);
    } catch (URISyntaxException e) {
      throw new IllegalArgumentException(e);
    }
    var refreshFutures =
        List.of(
            CompletableFuture.runAsync(Containers::prepareResourcesIndex),
            CompletableFuture.runAsync(Containers::prepareTicketsIndex),
            CompletableFuture.runAsync(Containers::prepareImportCandidateIndex));

    CompletableFuture.allOf(refreshFutures.toArray(new CompletableFuture[0])).join();
  }

  private static void prepareResourcesIndex() {
    try {
      createIndex(RESOURCES, RESOURCE_MAPPINGS.asMap(), RESOURCE_SETTINGS.asMap());
      populateIndex(RESOURCE_DATASOURCE_JSON, RESOURCES);
      indexingClient.refreshIndex(RESOURCES);
    } catch (IOException e) {
      logger.error("Failed to prepare resources index", e);
    }
  }

  private static void prepareTicketsIndex() {
    try {
      createIndex(TICKETS, TICKET_MAPPINGS.asMap(), null);
      populateIndex(TICKET_DATASOURCE_JSON, TICKETS);
      indexingClient.refreshIndex(TICKETS);
    } catch (IOException e) {
      logger.error("Failed to prepare tickets index", e);
    }
  }

  private static void prepareImportCandidateIndex() {
    try {
      createIndex(IMPORT_CANDIDATES_INDEX, IMPORT_CANDIDATE_MAPPINGS.asMap(), null);
      populateIndex(IMPORT_CANDIDATE_DATASOURCE_JSON, IMPORT_CANDIDATES_INDEX);
      indexingClient.refreshIndex(IMPORT_CANDIDATES_INDEX);
    } catch (IOException e) {
      logger.error("Failed to prepare import candidate index", e);
    }
  }

  public static void afterAll() throws Exception {

    logger.info("Stopping container");
    indexingClient.deleteIndex(RESOURCES);
    indexingClient.deleteIndex(TICKETS);
    indexingClient.deleteIndex(IMPORT_CANDIDATES_INDEX);
    Thread.sleep(DELAY_AFTER_INDEXING);
    container.stop();
  }

  public static Map<String, Object> loadMapFromResource(String resource) {
    var mappingsJson = stringFromResources(Path.of(resource));
    var type = new TypeReference<Map<String, Object>>() {};
    return attempt(() -> JsonUtils.dtoObjectMapper.readValue(mappingsJson, type)).orElseThrow();
  }

  private static void populateIndex(String resourcePath, String indexName) {
    var jsonFile = stringFromResources(Path.of(resourcePath));
    var jsonNodes = attempt(() -> JsonUtils.dtoObjectMapper.readTree(jsonFile)).orElseThrow();

    jsonNodes.forEach(jsonNode -> addDocumentToIndex(indexName, jsonNode));
  }

  private static void addDocumentToIndex(String indexName, JsonNode node) {
    try {
      var identifier =
          node.has(IDENTIFIER)
              ? new SortableIdentifier(node.get(IDENTIFIER).asText())
              : SortableIdentifier.next();
      var attributes = new EventConsumptionAttributes(indexName, identifier);
      indexingClient.addDocumentToIndex(new IndexDocument(attributes, node));
    } catch (Exception e) {
      logger.error(e.getMessage(), e.getCause());
    }
  }

  private static void createIndex(
      String indexName, Map<String, Object> mappings, Map<String, Object> settings)
      throws IOException {
    if (nonNull(mappings) && nonNull(settings)) {
      indexingClient.createIndex(indexName, mappings, settings);
    } else if (nonNull(mappings)) {
      indexingClient.createIndex(indexName, mappings);
    } else {
      indexingClient.createIndex(indexName);
    }
  }
}
