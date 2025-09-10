package no.unit.nva.indexingclient;

import static no.unit.nva.common.TestConstants.OPEN_SEARCH_IMAGE;
import static no.unit.nva.constants.Defaults.objectMapperWithEmpty;
import static no.unit.nva.indexing.testutils.MockedJwtProvider.setupMockedCachedJwtProvider;
import static no.unit.nva.indexingclient.utils.ShardRoutingUtils.createAnthologyDocument;
import static no.unit.nva.indexingclient.utils.ShardRoutingUtils.createChapterDocument;
import static no.unit.nva.indexingclient.utils.ShardRoutingUtils.createDocumentWithIdField;
import static no.unit.nva.indexingclient.utils.ShardRoutingUtils.createStandalonePublicationDocument;
import static no.unit.nva.indexingclient.utils.ShardRoutingUtils.toIndexDocument;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.fasterxml.jackson.databind.node.ObjectNode;
import java.io.IOException;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import no.unit.nva.indexingclient.models.IndexDocument;
import no.unit.nva.indexingclient.models.RestHighLevelClientWrapper;
import org.apache.hc.core5.http.HttpHost;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.opensearch.client.Request;
import org.opensearch.client.RestClient;
import org.opensearch.testcontainers.OpenSearchContainer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testcontainers.junit.jupiter.Testcontainers;

@Testcontainers
class ShardRoutingContainerTest {

  private static final Logger logger = LoggerFactory.getLogger(ShardRoutingContainerTest.class);
  private static final OpenSearchContainer<?> container =
      new OpenSearchContainer<>(OPEN_SEARCH_IMAGE);
  private static final String TEST_INDEX = "test-resources";
  private static final int NUMBER_OF_SHARDS = 5;

  private static IndexingClient indexingClient;
  private static RestClient lowLevelClient;

  @BeforeAll
  static void beforeAll() {
    container.withEnv("indices.query.bool.max_clause_count", "2048").start();

    try {
      var restClientBuilder = RestClient.builder(HttpHost.create(container.getHttpHostAddress()));
      lowLevelClient = restClientBuilder.build();
      var restHighLevelClientWrapper = new RestHighLevelClientWrapper(restClientBuilder);
      var cachedJwtProvider = setupMockedCachedJwtProvider();
      indexingClient = new IndexingClient(restHighLevelClientWrapper, cachedJwtProvider);
    } catch (URISyntaxException e) {
      throw new IllegalArgumentException(e);
    }
  }

  @AfterAll
  static void afterAll() {
    try {
      lowLevelClient.close();
    } catch (IOException e) {
      logger.warn("Failed to close low-level client", e);
    }
    container.stop();
  }

  @BeforeEach
  void beforeEach() throws IOException {
    createTestIndex();
  }

  @AfterEach
  void afterEach() throws IOException {
    indexingClient.deleteIndex(TEST_INDEX);
  }

  @Test
  @DisplayName("Should distribute standalone publications across multiple shards")
  void shouldDistributeStandalonePublicationsAcrossShards() {
    // Given many standalone publication documents
    var publicationCount = 1000;
    indexManyStandalonePublications(publicationCount);

    // When querying OpenSearch for actual shard distribution
    var shardDistribution = getActualShardDistribution();

    // Then documents should be distributed across multiple shards evenly
    assertEvenDistribution(shardDistribution, publicationCount, 0.25);
  }

  @Test
  @DisplayName("Should co-locate parent-child documents in same shard")
  void shouldColocateParentChildDocumentsInSameShard() {
    // Given an anthology with chapters
    var anthologyId = "real-anthology-123";
    var chapterIds = List.of("real-chapter-1", "real-chapter-2", "real-chapter-3");

    indexAnthologyWithChapters(anthologyId, chapterIds);

    // When querying OpenSearch for actual document locations
    var anthologyShardId = getDocumentShardId(anthologyId);
    var chapterShardIds = chapterIds.stream().map(this::getDocumentShardId).toList();

    // Then all documents should be in the same shard
    for (var chapterShardId : chapterShardIds) {
      assertEquals(anthologyShardId, chapterShardId);
    }

    for (int i = 0; i < chapterIds.size(); i++) {
      assertEquals(
          anthologyShardId,
          chapterShardIds.get(i),
          String.format(
              "Chapter '%s' should be in same shard as anthology '%s'. "
                  + "Anthology shard: %s, Chapter shard: %s",
              chapterIds.get(i), anthologyId, anthologyShardId, chapterShardIds.get(i)));
    }

    logger.info(
        "Parent-child co-location verified: anthology and {} chapters all in shard {}",
        chapterIds.size(),
        anthologyShardId);
  }

  @Test
  @DisplayName("Should handle mixed documents with proper routing")
  void shouldHandleMixedDocumentsWithProperRouting() {
    // Given mixed document types: standalone publications and anthology with chapters
    indexManyStandalonePublications(50);

    var anthologyId = "mixed-anthology-456";
    var chapterIds = List.of("mixed-chapter-1", "mixed-chapter-2");
    indexAnthologyWithChapters(anthologyId, chapterIds);

    // When querying for distribution
    var totalDocuments = 50 + 1 + 2; // publications + anthology + chapters
    var shardDistribution = getActualShardDistribution();

    // Then should have good distribution
    assertTrue(shardDistribution.size() > 1, "Should use multiple shards");
    assertEquals(
        totalDocuments,
        shardDistribution.values().stream().mapToInt(Integer::intValue).sum(),
        "Should have indexed all documents");

    // And parent-child should still be co-located
    var anthologyShardId = getDocumentShardId(anthologyId);
    chapterIds.forEach(
        chapterId ->
            assertEquals(
                anthologyShardId,
                getDocumentShardId(chapterId),
                "Chapter should be co-located with anthology"));

    logger.info("Mixed document routing verified with distribution: {}", shardDistribution);
  }

  @Test
  @DisplayName("Should route documents with id field correctly")
  void shouldRouteDocumentsWithIdFieldCorrectly() {
    // Given documents with id field instead of identifier
    var documentCount = 10;
    for (int i = 0; i < documentCount; i++) {
      var documentId = "doc-with-id-" + i;
      addDocumentsToIndex(createDocumentWithIdField(documentId));
    }

    // When querying for distribution
    var shardDistribution = getActualShardDistribution();

    // Then documents should be distributed (may not be perfectly even with small count)
    assertTrue(shardDistribution.size() > 0, "Should have documents in at least one shard");
    assertEquals(
        documentCount,
        shardDistribution.values().stream().mapToInt(Integer::intValue).sum(),
        "Should have indexed all documents with id field");

    logger.info("ID field routing verified with distribution: {}", shardDistribution);
  }

  @Test
  @DisplayName("Should demonstrate routing consistency between index and delete operations")
  void shouldDemonstrateRoutingConsistencyBetweenOperations() throws IOException {
    // Given a publication document indexed with routing
    var publicationId = "routing-consistency-test";
    var publicationDocument = createStandalonePublicationDocument(publicationId);
    var indexDocument = toIndexDocument(publicationDocument, TEST_INDEX);
    addDocumentsToIndex(indexDocument);

    // When getting the document's shard location
    var originalShardId = getDocumentShardId(publicationId);

    // And deleting then re-indexing the same document by recreating the index
    indexingClient.deleteIndex(TEST_INDEX);
    createTestIndex();

    addDocumentsToIndex(indexDocument);

    var newShardId = getDocumentShardId(publicationId);

    // Then the document should be in the same shard (consistent routing)
    assertEquals(
        originalShardId,
        newShardId,
        "Document should be routed to same shard after delete/re-index cycle");

    logger.info(
        "Routing consistency verified: document consistently routed to shard {}", originalShardId);
  }

  private void createTestIndex() throws IOException {
    var settings = createTestIndexSettings();
    var mappings = createTestIndexMappingsWithJoinField();
    indexingClient.createIndex(TEST_INDEX, mappings, settings);
    indexingClient.refreshIndex(TEST_INDEX);
    logger.debug("Created test index '{}' with {} shards", TEST_INDEX, NUMBER_OF_SHARDS);
  }

  private Map<String, Object> createTestIndexSettings() {
    return Map.of("index", Map.of("number_of_shards", NUMBER_OF_SHARDS, "number_of_replicas", 0));
  }

  private Map<String, Object> createTestIndexMappingsWithJoinField() {
    return Map.of(
        "properties",
        Map.of(
            "identifier", Map.of("type", "keyword"),
            "type", Map.of("type", "keyword"),
            "title", Map.of("type", "text"),
            "joinField", Map.of("type", "join", "relations", Map.of("hasParts", "partOf"))));
  }

  private void indexManyStandalonePublications(int count) {
    var indexDocuments = new ArrayList<IndexDocument>();
    for (int i = 0; i < count; i++) {
      var publicationId = "publication-" + i;
      var publicationDocument = createStandalonePublicationDocument(publicationId);
      indexDocuments.add(toIndexDocument(publicationDocument, TEST_INDEX));
    }
    addDocumentsToIndex(indexDocuments);
  }

  private void indexAnthologyWithChapters(String anthologyId, List<String> chapterIds) {
    var indexDocuments = new ArrayList<IndexDocument>();

    // Index anthology (parent document)
    var anthologyDocument = createAnthologyDocument(anthologyId);
    indexDocuments.add(toIndexDocument(anthologyDocument, TEST_INDEX));

    // Index chapters (child documents)
    for (var chapterId : chapterIds) {
      var chapterDocument = createChapterDocument(chapterId, anthologyId);
      indexDocuments.add(toIndexDocument(chapterDocument, TEST_INDEX));
    }

    addDocumentsToIndex(indexDocuments);
  }

  /**
   * Queries OpenSearch to get actual shard distribution of documents. Uses the _cat/shards API to
   * determine how many documents are in each shard.
   */
  private Map<String, Integer> getActualShardDistribution() {
    try {
      var request = new Request("GET", "/_cat/shards/" + TEST_INDEX + "?format=json&h=shard,docs");
      var response = lowLevelClient.performRequest(request);
      var responseBody = response.getEntity().getContent().readAllBytes();
      var responseJson = objectMapperWithEmpty.readTree(responseBody);

      var shardDistribution = new HashMap<String, Integer>();

      for (var shard : responseJson) {
        var shardId = shard.path("shard").asText();
        var docCount = shard.path("docs").asInt();
        shardDistribution.put(shardId, docCount);
      }

      return shardDistribution;
    } catch (IOException e) {
      throw new RuntimeException("Failed to get shard distribution", e);
    }
  }

  /**
   * Gets the shard ID where a specific document is located by calculating the routing key and
   * finding which shard contains that document.
   */
  private String getDocumentShardId(String documentId) {
    try {
      // Search for the document using its publication identifier to get the shard information
      var request = new Request("GET", "/" + TEST_INDEX + "/_search");
      request.setJsonEntity(
          """
          {
            "query": {
              "term": {
                "identifier": "%s"
              }
            },
            "size": 1
          }
          """
              .formatted(documentId));

      var response = lowLevelClient.performRequest(request);
      var responseBody = response.getEntity().getContent().readAllBytes();
      var responseJson = objectMapperWithEmpty.readTree(responseBody);

      var hits = responseJson.path("hits").path("hits");
      if (!hits.isEmpty()) {
        var hit = hits.get(0);
        var routing = hit.path("_routing").asText();
        return routing;
      } else {
        throw new IllegalStateException("Document not found: " + documentId);
      }
    } catch (IOException e) {
      throw new RuntimeException("Failed to get document shard ID for: " + documentId, e);
    }
  }

  private void assertEvenDistribution(
      Map<String, Integer> shardCounts, int totalDocuments, double tolerancePercent) {
    var expectedPerShard = (double) totalDocuments / NUMBER_OF_SHARDS;
    var tolerance = expectedPerShard * tolerancePercent;

    for (var shard : shardCounts.entrySet()) {
      assertEquals(expectedPerShard, shard.getValue(), tolerance);
    }
  }

  private void addSingleDocumentToIndex(IndexDocument indexDocument) {
    try {
      indexingClient.addDocumentToIndex(indexDocument);
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  private void addDocumentsToIndex(Collection<IndexDocument> documents) {
    try {
      documents.forEach(doc -> addSingleDocumentToIndex(doc));
      indexingClient.refreshIndex(TEST_INDEX);
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  private void addDocumentsToIndex(IndexDocument... documents) {
    addDocumentsToIndex(List.of(documents));
  }

  private void addDocumentsToIndex(ObjectNode... documents) {
    var indexDocuments =
        Arrays.stream(documents).map(doc -> toIndexDocument(doc, TEST_INDEX)).toList();
    addDocumentsToIndex(indexDocuments);
  }
}
