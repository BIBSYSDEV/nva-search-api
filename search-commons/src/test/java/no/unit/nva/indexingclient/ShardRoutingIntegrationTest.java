package no.unit.nva.indexingclient;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.util.*;
import no.unit.nva.identifiers.SortableIdentifier;
import no.unit.nva.indexingclient.models.EventConsumptionAttributes;
import no.unit.nva.indexingclient.models.IndexDocument;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Integration tests demonstrating that parent-child documents are co-located in the same shard
 * while achieving even distribution across all shards.
 */
class ShardRoutingIntegrationTest {

  private static final Logger logger = LoggerFactory.getLogger(ShardRoutingIntegrationTest.class);
  private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
  private static final int NUMBER_OF_SHARDS = 5;
  private static final String RESOURCES_INDEX = "resources";

  private ShardRoutingService shardRoutingService;

  @BeforeEach
  void setUp() {
    shardRoutingService = new ShardRoutingService(NUMBER_OF_SHARDS);
  }

  @Test
  @DisplayName("Should ensure anthology and chapter documents are co-located in same shard")
  void shouldEnsureParentChildCoLocation() {
    // Given an anthology (parent document)
    var anthologyId = "anthology-12345";
    var anthologyDocument = createAnthologyDocument(anthologyId);
    var anthologyIndexDocument =
        new IndexDocument(
            new EventConsumptionAttributes(RESOURCES_INDEX, SortableIdentifier.next()),
            anthologyDocument);

    // And a chapter (child document) that belongs to the anthology
    var chapterId = "chapter-67890";
    var chapterDocument = createChapterDocument(chapterId, anthologyId);
    var chapterIndexDocument =
        new IndexDocument(
            new EventConsumptionAttributes(RESOURCES_INDEX, SortableIdentifier.next()),
            chapterDocument);

    // When calculating routing keys for both documents
    var anthologyIndexRequest = anthologyIndexDocument.toIndexRequest(shardRoutingService);
    var chapterIndexRequest = chapterIndexDocument.toIndexRequest(shardRoutingService);

    // Then both should be routed to the same shard
    assertEquals(
        anthologyIndexRequest.routing(),
        chapterIndexRequest.routing(),
        "Anthology and chapter must be in the same shard for join operations");

    // And the routing key should be based on the anthology identifier
    var expectedRoutingKey = String.valueOf(Math.abs(anthologyId.hashCode()) % NUMBER_OF_SHARDS);
    assertEquals(
        expectedRoutingKey,
        anthologyIndexRequest.routing(),
        "Anthology routing should be based on its identifier");
    assertEquals(
        expectedRoutingKey,
        chapterIndexRequest.routing(),
        "Chapter routing should be based on parent anthology identifier");
  }

  @Test
  @DisplayName("Should generate valid shard IDs for standalone publications")
  void shouldGenerateValidShardIds() {
    // Given routing keys for many standalone publications
    var routingKeys = generateStandalonePublicationRoutingKeys(1000);

    // When validating shard IDs
    // Then all should be within valid range
    for (var routingKey : routingKeys) {
      var shardId = Integer.parseInt(routingKey);
      assertTrue(
          shardId >= 0 && shardId < NUMBER_OF_SHARDS,
          "Shard ID "
              + shardId
              + " should be within valid range [0-"
              + (NUMBER_OF_SHARDS - 1)
              + "]");
    }
  }

  @Test
  @DisplayName("Should distribute standalone publications across all available shards")
  void shouldDistributeAcrossAllShards() {
    // Given routing keys for many standalone publications
    var routingKeys = generateStandalonePublicationRoutingKeys(1000);

    // When analyzing shard usage
    var usedShards = new boolean[NUMBER_OF_SHARDS];
    for (var routingKey : routingKeys) {
      var shardId = Integer.parseInt(routingKey);
      usedShards[shardId] = true;
    }

    // Then all shards should be utilized
    for (int i = 0; i < NUMBER_OF_SHARDS; i++) {
      assertTrue(usedShards[i], "Shard " + i + " should have at least one document");
    }
  }

  @Test
  @DisplayName("Should achieve reasonably even distribution of standalone publications")
  void shouldAchieveEvenDistribution() {
    // Given routing keys for many standalone publications
    var routingKeys = generateStandalonePublicationRoutingKeys(1000);

    // When analyzing distribution
    var shardCounts = new HashMap<String, Integer>();
    for (var routingKey : routingKeys) {
      shardCounts.merge(routingKey, 1, Integer::sum);
    }

    // Then distribution should be reasonably even
    assertEvenDistribution(shardCounts, 1000, 0.25);
  }

  @Test
  @DisplayName("Should handle mixed scenario with multiple anthologies and chapters")
  void shouldHandleMixedScenarioWithMultipleAnthologiesAndChapters() {
    // Given multiple anthologies with their chapters
    var anthologyIds = new String[] {"anthology-A", "anthology-B", "anthology-C"};
    var chaptersPerAnthology = 3;

    // When processing all documents
    for (var anthologyId : anthologyIds) {
      // Create anthology document
      var anthologyDocument = createAnthologyDocument(anthologyId);
      var anthologyIndexDocument =
          new IndexDocument(
              new EventConsumptionAttributes(RESOURCES_INDEX, SortableIdentifier.next()),
              anthologyDocument);
      var anthologyRequest = anthologyIndexDocument.toIndexRequest(shardRoutingService);

      var expectedShardForAnthology = anthologyRequest.routing();

      // Create chapter documents for this anthology
      for (int j = 0; j < chaptersPerAnthology; j++) {
        var chapterId = anthologyId + "-chapter-" + j;
        var chapterDocument = createChapterDocument(chapterId, anthologyId);
        var chapterIndexDocument =
            new IndexDocument(
                new EventConsumptionAttributes(RESOURCES_INDEX, SortableIdentifier.next()),
                chapterDocument);
        var chapterRequest = chapterIndexDocument.toIndexRequest(shardRoutingService);

        // Then each chapter should be in the same shard as its anthology
        assertEquals(
            expectedShardForAnthology,
            chapterRequest.routing(),
            String.format(
                "Chapter %s should be in same shard as anthology %s", chapterId, anthologyId));
      }
    }
  }

  @Test
  @DisplayName("Should use consistent routing for delete operations")
  void shouldUseConsistentRoutingForDeleteOperations() {
    // Given a publication document
    var publicationId = "publication-for-delete";
    var publicationDocument = createStandalonePublicationDocument(publicationId);
    var indexDocument =
        new IndexDocument(
            new EventConsumptionAttributes(RESOURCES_INDEX, SortableIdentifier.next()),
            publicationDocument);

    // When calculating routing for index operations
    var indexRequest = indexDocument.toIndexRequest(shardRoutingService);
    var expectedRoutingKey = String.valueOf(Math.abs(publicationId.hashCode()) % NUMBER_OF_SHARDS);

    // Then routing should be consistent and predictable
    assertEquals(
        expectedRoutingKey,
        indexRequest.routing(),
        "Routing should be consistent for documents with same identifier");
  }

  private ObjectNode createAnthologyDocument(String anthologyId) {
    var document = OBJECT_MAPPER.createObjectNode();
    document.put("identifier", anthologyId);
    document.put("type", "Anthology");
    document.put("title", "Anthology: " + anthologyId);
    return document;
  }

  private ObjectNode createChapterDocument(String chapterId, String parentAnthologyId) {
    var document = OBJECT_MAPPER.createObjectNode();
    document.put("identifier", chapterId);
    document.put("type", "Chapter");
    document.put("title", "Chapter: " + chapterId);

    // Add joinField to indicate parent-child relationship
    var joinField = OBJECT_MAPPER.createObjectNode();
    joinField.put("name", "partOf");
    joinField.put("parent", parentAnthologyId);
    document.set("joinField", joinField);

    return document;
  }

  private ObjectNode createStandalonePublicationDocument(String publicationId) {
    var document = OBJECT_MAPPER.createObjectNode();
    document.put("identifier", publicationId);
    document.put("type", "Publication");
    document.put("title", "Publication: " + publicationId);
    return document;
  }

  /**
   * Generates routing keys for many standalone publications.
   *
   * @param count number of publications to generate routing keys for
   * @return list of routing keys
   */
  private List<String> generateStandalonePublicationRoutingKeys(int count) {
    var routingKeys = new ArrayList<String>();

    for (int i = 0; i < count; i++) {
      var publicationId = "publication-" + i;
      var publicationDocument = createStandalonePublicationDocument(publicationId);
      var indexDocument =
          new IndexDocument(
              new EventConsumptionAttributes(RESOURCES_INDEX, SortableIdentifier.next()),
              publicationDocument);

      var indexRequest = indexDocument.toIndexRequest(shardRoutingService);
      routingKeys.add(indexRequest.routing());
    }

    return routingKeys;
  }

  @ParameterizedTest
  @ValueSource(ints = {100, 1_000})
  @DisplayName("Should achieve dynamic distribution across all shards")
  void shouldAchieveDynamicDistributionAcrossAllShards(int totalDocuments) {
    // Given many different publication documents
    var routingKeys = new ArrayList<String>();
    for (int i = 0; i < totalDocuments; i++) {
      var publicationDocument = createStandalonePublicationDocument("publication-" + i);
      var indexDocument =
          new IndexDocument(
              new EventConsumptionAttributes(RESOURCES_INDEX, SortableIdentifier.next()),
              publicationDocument);

      var indexRequest = indexDocument.toIndexRequest(shardRoutingService);
      routingKeys.add(indexRequest.routing());
    }

    // Then documents should be distributed across all shards
    var shardCounts = countDocumentsPerShard(routingKeys);
    assertEquals(
        NUMBER_OF_SHARDS,
        shardCounts.size(),
        String.format("Should use all shards. Actual distribution: %s", shardCounts));

    // And distribution should be reasonably even (within 25% tolerance)
    assertEvenDistribution(shardCounts, totalDocuments, 0.25);

    // Log distribution for verification
    logger.info("Dynamic sharding distribution across {} documents:", totalDocuments);
    for (int i = 0; i < NUMBER_OF_SHARDS; i++) {
      var count = shardCounts.get(String.valueOf(i));
      logger.info("  Shard {}: {} documents", i, count);
    }
  }

  private Map<String, Integer> countDocumentsPerShard(Collection<String> routingKeys) {
    var shardCounts = new HashMap<String, Integer>();
    for (var routingKey : routingKeys) {
      shardCounts.merge(routingKey, 1, Integer::sum);
    }
    return shardCounts;
  }

  /**
   * Asserts that document distribution across shards is reasonably even.
   *
   * @param shardCounts map of shard ID to document count
   * @param totalDocuments total number of documents
   * @param tolerancePercent acceptable deviation from expected distribution (e.g., 0.3 for 30%)
   */
  private void assertEvenDistribution(
      Map<String, Integer> shardCounts, int totalDocuments, double tolerancePercent) {
    var expectedPerShard = (double) totalDocuments / NUMBER_OF_SHARDS;
    var tolerance = expectedPerShard * tolerancePercent;

    shardCounts.values().forEach(count -> assertEquals(count, expectedPerShard, tolerance));
  }
}
