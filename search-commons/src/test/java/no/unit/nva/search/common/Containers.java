package no.unit.nva.search.common;

import static java.util.Objects.nonNull;
import static no.unit.nva.indexing.testutils.MockedJwtProvider.setupMockedCachedJwtProvider;
import static no.unit.nva.indexingclient.constants.ApplicationConstants.IMPORT_CANDIDATES_INDEX;
import static no.unit.nva.search.common.Constants.DELAY_AFTER_INDEXING;
import static no.unit.nva.search.common.Constants.OPEN_SEARCH_IMAGE;
import static no.unit.nva.search.common.constant.Words.IDENTIFIER;
import static no.unit.nva.search.common.constant.Words.RESOURCES;
import static no.unit.nva.search.common.constant.Words.TICKETS;
import static nva.commons.core.attempt.Try.attempt;
import static nva.commons.core.ioutils.IoUtils.stringFromResources;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Map;
import no.unit.nva.commons.json.JsonUtils;
import no.unit.nva.identifiers.SortableIdentifier;
import no.unit.nva.indexingclient.IndexingClient;
import no.unit.nva.indexingclient.models.EventConsumptionAttributes;
import no.unit.nva.indexingclient.models.IndexDocument;
import no.unit.nva.indexingclient.models.RestHighLevelClientWrapper;
import org.apache.http.HttpHost;
import org.opensearch.client.RestClient;
import org.opensearch.testcontainers.OpensearchContainer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testcontainers.junit.jupiter.Testcontainers;

@Testcontainers
public class Containers {

  public static final OpensearchContainer container = new OpensearchContainer(OPEN_SEARCH_IMAGE);
  public static final String IMPORT_CANDIDATE_MAPPING_DEV_JSON =
      "import_candidate_mappings_dev.json";
  private static final Logger logger = LoggerFactory.getLogger(Containers.class);

  private static final String RESOURCE_DATASOURCE_JSON = "resource_datasource.json";
  private static final String RESOURCE_MAPPING_DEV_JSON = "resource_mappings_dev.json";
  private static final String RESOURCE_SETTING_DEV_JSON = "resource_settings_dev.json";

  private static final String TICKET_DATASOURCE_JSON = "ticket_datasource.json";
  private static final String TICKET_MAPPING_DEV_JSON = "ticket_mappings_dev.json";

  private static final String IMPORT_CANDIDATE_DATASOURCE_JSON = "import_candidate_datasource.json";
  public static IndexingClient indexingClient;

  public static void setup() throws IOException, InterruptedException {
    container.start();

    var restClientBuilder = RestClient.builder(HttpHost.create(container.getHttpHostAddress()));
    var restHighLevelClientWrapper = new RestHighLevelClientWrapper(restClientBuilder);
    var cachedJwtProvider = setupMockedCachedJwtProvider();
    indexingClient = new IndexingClient(restHighLevelClientWrapper, cachedJwtProvider);

    logger.info("creating indexes");

    createIndex(TICKETS, TICKET_MAPPING_DEV_JSON, null);
    createIndex(IMPORT_CANDIDATES_INDEX, IMPORT_CANDIDATE_MAPPING_DEV_JSON, null);
    createIndex(RESOURCES, RESOURCE_MAPPING_DEV_JSON, RESOURCE_SETTING_DEV_JSON);

    logger.info("populating indexes");

    populateIndex(TICKET_DATASOURCE_JSON, TICKETS);
    populateIndex(IMPORT_CANDIDATE_DATASOURCE_JSON, IMPORT_CANDIDATES_INDEX);
    populateIndex(RESOURCE_DATASOURCE_JSON, RESOURCES);

    logger.info("Waiting {} ms for indexing to complete", DELAY_AFTER_INDEXING);
    Thread.sleep(DELAY_AFTER_INDEXING);
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

  private static void createIndex(String indexName, String mappingsPath, String settingsPath)
      throws IOException {
    var mappings = nonNull(mappingsPath) ? loadMapFromResource(mappingsPath) : null;
    var settings = nonNull(settingsPath) ? loadMapFromResource(settingsPath) : null;

    if (nonNull(mappings) && nonNull(settings)) {
      indexingClient.createIndex(indexName, mappings, settings);
    } else if (nonNull(mappings)) {
      indexingClient.createIndex(indexName, mappings);
    } else {
      indexingClient.createIndex(indexName);
    }
  }
}
