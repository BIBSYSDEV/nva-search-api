package no.unit.nva.indexingclient;

import static no.unit.nva.indexingclient.utils.ShardRoutingUtils.createAnthologyDocument;
import static no.unit.nva.indexingclient.utils.ShardRoutingUtils.createChapterDocument;
import static no.unit.nva.indexingclient.utils.ShardRoutingUtils.createDocumentWithBothIdentifierAndId;
import static no.unit.nva.indexingclient.utils.ShardRoutingUtils.createDocumentWithIdField;
import static no.unit.nva.indexingclient.utils.ShardRoutingUtils.createStandalonePublicationDocument;
import static org.junit.jupiter.api.Assertions.assertEquals;

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
  private static final String RESOURCES_INDEX = "resources";

  private ShardRoutingService shardRoutingService;

  @BeforeEach
  void setUp() {
    shardRoutingService = new ShardRoutingService();
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

    // And the routing key should be the anthology identifier itself
    assertEquals(
        anthologyId,
        anthologyIndexRequest.routing(),
        "Anthology routing should be based on its identifier");
    assertEquals(
        anthologyId,
        chapterIndexRequest.routing(),
        "Chapter routing should be based on parent anthology identifier");
  }

  @Test
  @DisplayName("Should generate unique routing keys for different publications")
  void shouldGenerateUniqueRoutingKeys() {
    // Given routing keys for many standalone publications
    var routingKeys = generateStandalonePublicationRoutingKeys(1000);

    // When analyzing routing key uniqueness
    var uniqueRoutingKeys = new HashSet<>(routingKeys);

    // Then all routing keys should be unique (each publication has unique identifier)
    assertEquals(
        routingKeys.size(),
        uniqueRoutingKeys.size(),
        "All routing keys should be unique since each publication has unique identifier");
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

    // Then routing should be consistent and predictable
    assertEquals(
        publicationId,
        indexRequest.routing(),
        "Routing should be consistent for documents with same identifier");
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
  @DisplayName("Should generate consistent routing keys for publications")
  void shouldGenerateConsistentRoutingKeysForPublications(int totalDocuments) {
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

    // Then routing keys should match publication identifiers
    for (int i = 0; i < totalDocuments; i++) {
      var expectedId = "publication-" + i;
      assertEquals(
          expectedId, routingKeys.get(i), "Routing key should match publication identifier");
    }

    // And all routing keys should be unique
    var uniqueRoutingKeys = new HashSet<>(routingKeys);
    assertEquals(totalDocuments, uniqueRoutingKeys.size(), "All routing keys should be unique");

    logger.info(
        "Generated {} consistent routing keys for {} documents",
        uniqueRoutingKeys.size(),
        totalDocuments);
  }

  @Test
  @DisplayName("Should use id field when identifier field is not present")
  void shouldUseIdFieldWhenIdentifierNotPresent() {
    // Given a document with id field but no identifier field
    var documentId = "doc-with-id-123";
    var documentWithId = createDocumentWithIdField(documentId);
    var indexDocument =
        new IndexDocument(
            new EventConsumptionAttributes(RESOURCES_INDEX, SortableIdentifier.next()),
            documentWithId);

    // When calculating routing key
    var indexRequest = indexDocument.toIndexRequest(shardRoutingService);

    // Then routing key should be the id value
    assertEquals(
        documentId,
        indexRequest.routing(),
        "Routing should use id field when identifier is not present");
  }

  @Test
  @DisplayName("Should prefer identifier over id field when both are present")
  void shouldPreferIdentifierOverIdField() {
    // Given a document with both identifier and id fields
    var identifierValue = "identifier-123";
    var idValue = "id-456";
    var documentWithBoth = createDocumentWithBothIdentifierAndId(identifierValue, idValue);
    var indexDocument =
        new IndexDocument(
            new EventConsumptionAttributes(RESOURCES_INDEX, SortableIdentifier.next()),
            documentWithBoth);

    // When calculating routing key
    var indexRequest = indexDocument.toIndexRequest(shardRoutingService);

    // Then routing key should prefer identifier
    assertEquals(
        identifierValue,
        indexRequest.routing(),
        "Routing should prefer identifier field over id field");
  }
}
