package no.unit.nva.indexingclient;

import static java.util.UUID.randomUUID;
import static no.unit.nva.indexingclient.utils.ShardRoutingUtils.createExampleDocument;
import static no.unit.nva.indexingclient.utils.ShardRoutingUtils.createResourceAndGetRoutingKey;
import static no.unit.nva.indexingclient.utils.ShardRoutingUtils.createResourceWithJoinField;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class ShardRoutingServiceTest {

  private static final String IDENTIFIER_FIELD = "identifier";
  private static final String JOIN_FIELD = "joinField";
  private static final String JOIN_FIELD_PARENT = "parent";
  private static final String JOIN_FIELD_NAME = "name";
  private static final String PART_OF = "partOf";

  private ShardRoutingService shardRoutingService;

  @BeforeEach
  void setUp() {
    shardRoutingService = new ShardRoutingService();
  }

  @Test
  @DisplayName("Should use parent identifier for child documents with joinField")
  void shouldUseParentIdentifierForChildDocuments() {
    // Given a child document with joinField containing parent reference
    var parentIdentifier = randomUUID().toString();
    var resource = createResourceWithJoinField(parentIdentifier);
    resource.put(IDENTIFIER_FIELD, randomUUID().toString());

    // When calculating routing key
    var routingKey = shardRoutingService.calculateRoutingKey(resource);

    // Then should use parent identifier for routing
    assertEquals(parentIdentifier, routingKey);
  }

  @Test
  @DisplayName("Should use document identifier when no parent exists")
  void shouldUseDocumentIdentifierWhenNoParent() {
    // Given a document with only identifier field
    var identifier = randomUUID().toString();
    var resource = createExampleDocument();
    resource.put(IDENTIFIER_FIELD, identifier);

    // When calculating routing key
    var routingKey = shardRoutingService.calculateRoutingKey(resource);

    // Then should use document identifier for routing
    assertEquals(identifier, routingKey);
  }

  @Test
  @DisplayName("Should generate consistent routing key when no identifier or parent exists")
  void shouldGenerateConsistentRoutingKeyWhenNoIdentifierOrParent() {
    // Given a document without identifier or joinField
    var resource = createExampleDocument();
    resource.put("title", "Some document");

    // When calculating routing key multiple times
    var routingKey1 = shardRoutingService.calculateRoutingKey(resource);
    var routingKey2 = shardRoutingService.calculateRoutingKey(resource);

    // Then should generate valid and consistent shard ID
    assertNotNull(routingKey1);
    assertEquals(routingKey1, routingKey2);
  }

  @Test
  @DisplayName("Should handle null joinField parent gracefully")
  void shouldHandleNullJoinFieldParentGracefully() {
    // Given a document with joinField but null parent
    var identifier = "document-with-null-parent";
    var resource = createExampleDocument();
    resource.put(IDENTIFIER_FIELD, identifier);

    var joinField = createExampleDocument();
    joinField.put(JOIN_FIELD_NAME, PART_OF);
    joinField.putNull(JOIN_FIELD_PARENT);
    resource.set(JOIN_FIELD, joinField);

    // When calculating routing key
    var routingKey = shardRoutingService.calculateRoutingKey(resource);

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
    var routingKey = shardRoutingService.calculateRoutingKey(resource);

    // Then should fall back to identifier
    assertEquals(identifier, routingKey);
  }

  @Test
  @DisplayName("Should handle null identifier gracefully")
  void shouldHandleNullIdentifierGracefully() {
    // Given a document with null identifier
    var resource = createExampleDocument();
    resource.putNull(IDENTIFIER_FIELD);

    // When calculating routing key
    var routingKey = shardRoutingService.calculateRoutingKey(resource);

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
    var parentResource = createExampleDocument();
    parentResource.put(IDENTIFIER_FIELD, parentId);

    // Child document with joinField pointing to parent
    var childResource = createResourceWithJoinField(parentId);
    childResource.put(IDENTIFIER_FIELD, childId);

    // When calculating routing keys
    var parentRoutingKey = shardRoutingService.calculateRoutingKey(parentResource);
    var childRoutingKey = shardRoutingService.calculateRoutingKey(childResource);

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
  @DisplayName("Should handle complex joinField structure")
  void shouldHandleComplexJoinFieldStructure() {
    // Given a document with complex joinField structure
    var parentIdentifier = "complex-parent-123";
    var resource = createExampleDocument();
    resource.put(IDENTIFIER_FIELD, "child-document");

    var joinField = createExampleDocument();
    joinField.put(JOIN_FIELD_NAME, PART_OF);
    joinField.put(JOIN_FIELD_PARENT, parentIdentifier);
    joinField.put("someOtherField", "someValue");
    resource.set(JOIN_FIELD, joinField);

    // When calculating routing key
    var routingKey = shardRoutingService.calculateRoutingKey(resource);

    // Then should use parent identifier
    assertEquals(parentIdentifier, routingKey);
  }

  @Test
  @DisplayName("Should handle malformed joinField gracefully")
  void shouldHandleMalformedJoinFieldGracefully() {
    // Given a document with malformed joinField (not an object)
    var identifier = "document-with-malformed-join";
    var resource = createExampleDocument();
    resource.put(IDENTIFIER_FIELD, identifier);
    resource.put(JOIN_FIELD, "not-an-object");

    // When calculating routing key
    var routingKey = shardRoutingService.calculateRoutingKey(resource);

    // Then should fall back to identifier
    assertEquals(identifier, routingKey);
  }
}
