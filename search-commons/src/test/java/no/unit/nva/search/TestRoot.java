package no.unit.nva.search;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
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

import java.io.IOException;
import java.nio.file.Path;
import java.util.Map;

import static java.util.Objects.nonNull;
import static no.unit.nva.indexing.testutils.MockedJwtProvider.setupMockedCachedJwtProvider;
import static no.unit.nva.indexingclient.constants.ApplicationConstants.IMPORT_CANDIDATES_INDEX;
import static no.unit.nva.search.common.Constants.DELAY_AFTER_INDEXING;
import static no.unit.nva.search.common.Constants.OPEN_SEARCH_IMAGE;
import static no.unit.nva.search.common.constant.Words.RESOURCES;
import static no.unit.nva.search.common.constant.Words.TICKETS;
import static nva.commons.core.attempt.Try.attempt;
import static nva.commons.core.ioutils.IoUtils.stringFromResources;


//@Suite
//@SelectClasses({AFirstTest.class, ImportCandidateClientTest.class, ResourceClientTest.class, ScrollClientTest.class,
//    ResourceSearchQueryTest.class, TicketClientTest.class, UserSettingsClientTest.class, XTest.class})
@Testcontainers
public class TestRoot {


    public static IndexingClient indexingClient;
    public static final OpensearchContainer container = new OpensearchContainer(OPEN_SEARCH_IMAGE);

    private static final Logger logger = LoggerFactory.getLogger(TestRoot.class);

    private static final String SAMPLE_RESOURCES_SEARCH_JSON = "datasource_resources.json";
    private static final String SAMPLE_IMPORT_CANDIDATES = "datasource_import_candidates.json";
    private static final String SAMPLE_TICKETS_SEARCH_JSON = "datasource_tickets.json";

    private static final String TEST_TICKETS_MAPPINGS_JSON = "mapping_test_tickets.json";
    public static final String TEST_CANDIDATES_MAPPINGS_JSON = "mapping_test_import_candidates.json";
    private static final String TEST_RESOURCES_MAPPINGS_JSON = "mapping_test_resources.json";

    private static final String TEST_RESOURCES_SETTINGS_JSON = "setting_test_resources.json";


    public static void setup() throws IOException, InterruptedException {
        container.start();

        var restClientBuilder = RestClient.builder(HttpHost.create(container.getHttpHostAddress()));
        var restHighLevelClientWrapper = new RestHighLevelClientWrapper(restClientBuilder);
        var cachedJwtProvider = setupMockedCachedJwtProvider();
        indexingClient = new IndexingClient(restHighLevelClientWrapper, cachedJwtProvider);

        createIndex(RESOURCES, TEST_RESOURCES_MAPPINGS_JSON, TEST_RESOURCES_SETTINGS_JSON);
        createIndex(TICKETS, TEST_TICKETS_MAPPINGS_JSON, null);
        createIndex(IMPORT_CANDIDATES_INDEX, TEST_CANDIDATES_MAPPINGS_JSON, null);


        populateIndex(SAMPLE_RESOURCES_SEARCH_JSON, RESOURCES);
        populateIndex(SAMPLE_TICKETS_SEARCH_JSON, TICKETS);
        populateIndex(SAMPLE_IMPORT_CANDIDATES, IMPORT_CANDIDATES_INDEX);

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
        var type = new TypeReference<Map<String, Object>>() {
        };
        return attempt(() -> JsonUtils.dtoObjectMapper.readValue(mappingsJson, type)).orElseThrow();
    }

    private static void populateIndex(String resourcePath, String indexName) {
        var jsonFile = stringFromResources(Path.of(resourcePath));
        var jsonNodes =
            attempt(() -> JsonUtils.dtoObjectMapper.readTree(jsonFile)).orElseThrow();

        jsonNodes.forEach(jsonNode -> addDocumentToIndex(indexName, jsonNode));
    }

    private static void addDocumentToIndex(String indexName, JsonNode node) {
        try {
            var attributes = new EventConsumptionAttributes(indexName, SortableIdentifier.next());
            indexingClient.addDocumentToIndex(new IndexDocument(attributes, node));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private static void createIndex(String indexName, String mappingsPath, String settingsPath) throws IOException {
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
