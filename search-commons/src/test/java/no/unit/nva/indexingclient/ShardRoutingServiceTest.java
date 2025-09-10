package no.unit.nva.indexingclient;

import static java.util.UUID.randomUUID;
import static no.unit.nva.constants.Defaults.objectMapperWithEmpty;
import static no.unit.nva.indexingclient.utils.ShardRoutingUtils.IDENTIFIER_FIELD;
import static no.unit.nva.indexingclient.utils.ShardRoutingUtils.JOIN_FIELD;
import static no.unit.nva.indexingclient.utils.ShardRoutingUtils.JOIN_FIELD_NAME;
import static no.unit.nva.indexingclient.utils.ShardRoutingUtils.JOIN_FIELD_PARENT;
import static no.unit.nva.indexingclient.utils.ShardRoutingUtils.PART_OF;
import static no.unit.nva.indexingclient.utils.ShardRoutingUtils.createAnthologyDocument;
import static no.unit.nva.indexingclient.utils.ShardRoutingUtils.createChapterDocument;
import static no.unit.nva.indexingclient.utils.ShardRoutingUtils.createDocumentNode;
import static no.unit.nva.indexingclient.utils.ShardRoutingUtils.createDocumentWithBothIdentifierAndId;
import static no.unit.nva.indexingclient.utils.ShardRoutingUtils.createDocumentWithIdField;
import static no.unit.nva.indexingclient.utils.ShardRoutingUtils.createResourceAndGetRoutingKey;
import static no.unit.nva.indexingclient.utils.ShardRoutingUtils.createResourceWithJoinField;
import static no.unit.nva.indexingclient.utils.ShardRoutingUtils.createStandalonePublicationDocument;
import static no.unit.nva.indexingclient.utils.ShardRoutingUtils.toIndexDocument;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class ShardRoutingServiceTest {
  private static final String RESOURCES_INDEX = "resources";

  @Test
  @DisplayName("Should use parent identifier for child documents with joinField")
  void shouldUseParentIdentifierForChildDocuments() {
    // Given a child document with joinField containing parent reference
    var parentIdentifier = randomUUID().toString();
    var resource = createResourceWithJoinField(parentIdentifier);
    resource.put(IDENTIFIER_FIELD, randomUUID().toString());

    // When calculating routing key
    var routingKey = ShardRoutingService.calculateRoutingKey(resource);

    // Then should use parent identifier for routing
    assertEquals(parentIdentifier, routingKey);
  }

  @Test
  @DisplayName("Should use document identifier when no parent exists")
  void shouldUseDocumentIdentifierWhenNoParent() {
    // Given a document with only identifier field
    var identifier = randomUUID().toString();
    var resource = createDocumentNode();
    resource.put(IDENTIFIER_FIELD, identifier);

    // When calculating routing key
    var routingKey = ShardRoutingService.calculateRoutingKey(resource);

    // Then should use document identifier for routing
    assertEquals(identifier, routingKey);
  }

  @Test
  @DisplayName("Should generate consistent routing key when no identifier or parent exists")
  void shouldGenerateConsistentRoutingKeyWhenNoIdentifierOrParent() {
    // Given a document without identifier or joinField
    var resource = createDocumentNode();
    resource.put("title", "Some document");

    // When calculating routing key multiple times
    var routingKey1 = ShardRoutingService.calculateRoutingKey(resource);
    var routingKey2 = ShardRoutingService.calculateRoutingKey(resource);

    // Then should generate valid and consistent shard ID
    assertNotNull(routingKey1);
    assertEquals(routingKey1, routingKey2);
  }

  @Test
  @DisplayName("Should handle null joinField parent gracefully")
  void shouldHandleNullJoinFieldParentGracefully() {
    // Given a document with joinField but null parent
    var identifier = "document-with-null-parent";
    var resource = createDocumentNode();
    resource.put(IDENTIFIER_FIELD, identifier);

    var joinField = objectMapperWithEmpty.createObjectNode();
    joinField.put(JOIN_FIELD_NAME, PART_OF);
    joinField.putNull(JOIN_FIELD_PARENT);
    resource.set(JOIN_FIELD, joinField);

    // When calculating routing key
    var routingKey = ShardRoutingService.calculateRoutingKey(resource);

    // Then should fall back to identifier
    assertEquals(identifier, routingKey);
  }

  @Test
  @DisplayName("Should handle empty string parent gracefully")
  void shouldHandleEmptyStringParentGracefully() {
    // Given a document with empty string parent
    var identifier = "document-with-empty-parent";
    var resource = createResourceWithJoinField("");
    resource.put(IDENTIFIER_FIELD, identifier);

    // When calculating routing key
    var routingKey = ShardRoutingService.calculateRoutingKey(resource);

    // Then should fall back to identifier
    assertEquals(identifier, routingKey);
  }

  @Test
  @DisplayName("Should handle null identifier gracefully")
  void shouldHandleNullIdentifierGracefully() {
    // Given a document with null identifier
    var resource = createDocumentNode();
    resource.putNull(IDENTIFIER_FIELD);

    // When calculating routing key
    var routingKey = ShardRoutingService.calculateRoutingKey(resource);

    // Then should generate consistent routing key based on document hash
    assertEquals(resource.toString(), routingKey);
  }

  @Test
  @DisplayName("Should ensure parent and child documents have same routing key")
  void shouldEnsureParentAndChildHaveSameRoutingKey() {
    // Given parent and child documents
    var parentId = "anthology-123";
    var childId = "chapter-456";

    // Parent document
    var parentResource = createDocumentNode();
    parentResource.put(IDENTIFIER_FIELD, parentId);

    // Child document with joinField pointing to parent
    var childResource = createResourceWithJoinField(parentId);
    childResource.put(IDENTIFIER_FIELD, childId);

    // When calculating routing keys
    var parentRoutingKey = ShardRoutingService.calculateRoutingKey(parentResource);
    var childRoutingKey = ShardRoutingService.calculateRoutingKey(childResource);

    // Then both should have same routing key
    assertEquals(parentRoutingKey, childRoutingKey);
  }

  @Test
  @DisplayName("Should produce consistent routing keys for same identifier")
  void shouldProduceConsistentRoutingKeysForSameIdentifier() {
    // Given multiple documents with same identifier
    var identifier = "consistent-document";

    // When calculating routing key multiple times
    var routingKey1 = createResourceAndGetRoutingKey(identifier);
    var routingKey2 = createResourceAndGetRoutingKey(identifier);
    var routingKey3 = createResourceAndGetRoutingKey(identifier);

    // Then all should produce same routing key
    assertEquals(routingKey1, routingKey2);
    assertEquals(routingKey2, routingKey3);
  }

  @Test
  @DisplayName("Should handle malformed joinField gracefully")
  void shouldHandleMalformedJoinFieldGracefully() {
    // Given a document with malformed joinField (not an object)
    var identifier = "document-with-malformed-join";
    var resource = createDocumentNode();
    resource.put(IDENTIFIER_FIELD, identifier);
    resource.put(JOIN_FIELD, "not-an-object");

    // When calculating routing key
    var routingKey = ShardRoutingService.calculateRoutingKey(resource);

    // Then should fall back to identifier
    assertEquals(identifier, routingKey);
  }

  @Test
  @DisplayName("Should ensure anthology and chapter documents are co-located in same shard")
  void shouldEnsureParentChildCoLocation() {
    // Given an anthology (parent document)
    var anthologyId = "anthology-12345";
    var anthologyDocument = createAnthologyDocument(anthologyId);
    var anthologyIndexDocument = toIndexDocument(anthologyDocument, RESOURCES_INDEX);

    // And a chapter (child document) that belongs to the anthology
    var chapterId = "chapter-67890";
    var chapterDocument = createChapterDocument(chapterId, anthologyId);
    var chapterIndexDocument = toIndexDocument(chapterDocument, RESOURCES_INDEX);

    // When calculating routing keys for both documents
    var anthologyIndexRequest = anthologyIndexDocument.toIndexRequest();
    var chapterIndexRequest = chapterIndexDocument.toIndexRequest();

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
    var anthologyIds = List.of("anthology-A", "anthology-B", "anthology-C");
    var chaptersPerAnthology = 3;

    // When processing all documents
    for (var anthologyId : anthologyIds) {
      // Create anthology document
      var anthologyDocument = createAnthologyDocument(anthologyId);
      var anthologyIndexDocument = toIndexDocument(anthologyDocument, RESOURCES_INDEX);
      var anthologyRequest = anthologyIndexDocument.toIndexRequest();

      // Create chapter documents for this anthology
      for (int j = 0; j < chaptersPerAnthology; j++) {
        var chapterId = anthologyId + "-chapter-" + j;
        var chapterDocument = createChapterDocument(chapterId, anthologyId);
        var chapterIndexDocument = toIndexDocument(chapterDocument, RESOURCES_INDEX);
        var chapterRequest = chapterIndexDocument.toIndexRequest();

        // Then each chapter should be in the same shard as its anthology
        assertEquals(
            anthologyRequest.routing(),
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
    var publicationIdentifier = "publication-for-delete";
    var publicationDocument = createStandalonePublicationDocument(publicationIdentifier);
    var indexDocument = toIndexDocument(publicationDocument, RESOURCES_INDEX);

    // When calculating routing for index operations
    var indexRequest = indexDocument.toIndexRequest();

    // Then routing should be consistent and predictable
    assertEquals(
        publicationIdentifier,
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
      var indexDocument = toIndexDocument(publicationDocument, RESOURCES_INDEX);

      var indexRequest = indexDocument.toIndexRequest();
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
      var indexDocument = toIndexDocument(publicationDocument, RESOURCES_INDEX);

      var indexRequest = indexDocument.toIndexRequest();
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
  }

  @Test
  @DisplayName("Should use id field when identifier field is not present")
  void shouldUseIdFieldWhenIdentifierNotPresent() {
    // Given a document with id field but no identifier field
    var documentId = "doc-with-id-123";
    var documentWithId = createDocumentWithIdField(documentId);
    var indexDocument = toIndexDocument(documentWithId, RESOURCES_INDEX);

    // When calculating routing key
    var indexRequest = indexDocument.toIndexRequest();

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
    var indexDocument = toIndexDocument(documentWithBoth, RESOURCES_INDEX);

    // When calculating routing key
    var indexRequest = indexDocument.toIndexRequest();

    // Then routing key should prefer identifier
    assertEquals(
        identifierValue,
        indexRequest.routing(),
        "Routing should prefer identifier field over id field");
  }
}
