package no.unit.nva.indexingclient;

import static java.util.UUID.randomUUID;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.util.HashMap;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class ShardRoutingServiceTest {

  private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
  private static final int DEFAULT_NUMBER_OF_SHARDS = 5;
  private static final String IDENTIFIER_FIELD = "identifier";
  private static final String JOIN_FIELD = "joinField";
  private static final String JOIN_FIELD_PARENT = "parent";
  private static final String JOIN_FIELD_NAME = "name";
  private static final String PART_OF = "partOf";

  private ShardRoutingService shardRoutingService;

  @BeforeEach
  void setUp() {
    shardRoutingService = new ShardRoutingService(DEFAULT_NUMBER_OF_SHARDS);
  }

  @Test
  @DisplayName("Should use parent identifier for child documents with joinField")
  void shouldUseParentIdentifierForChildDocuments() {
    // Given a child document with joinField containing parent reference
    var parentId = randomUUID().toString();
    var resource = createResourceWithJoinField(parentId);
    resource.put(IDENTIFIER_FIELD, randomUUID().toString());

    // When calculating routing key
    var routingKey = shardRoutingService.calculateRoutingKey(resource);

    // Then should use parent identifier for routing
    var expectedRoutingKey =
        String.valueOf(Math.abs(parentId.hashCode()) % DEFAULT_NUMBER_OF_SHARDS);
    assertEquals(expectedRoutingKey, routingKey);
  }

  @Test
  @DisplayName("Should use document identifier when no parent exists")
  void shouldUseDocumentIdentifierWhenNoParent() {
    // Given a document with only identifier field
    var identifier = randomUUID().toString();
    var resource = OBJECT_MAPPER.createObjectNode();
    resource.put(IDENTIFIER_FIELD, identifier);

    // When calculating routing key
    var routingKey = shardRoutingService.calculateRoutingKey(resource);

    // Then should use document identifier for routing
    var expectedRoutingKey =
        String.valueOf(Math.abs(identifier.hashCode()) % DEFAULT_NUMBER_OF_SHARDS);
    assertEquals(expectedRoutingKey, routingKey);
  }

  @Test
  @DisplayName("Should generate consistent routing key when no identifier or parent exists")
  void shouldGenerateConsistentRoutingKeyWhenNoIdentifierOrParent() {
    // Given a document without identifier or joinField
    var resource = OBJECT_MAPPER.createObjectNode();
    resource.put("title", "Some document");

    // When calculating routing key multiple times
    var routingKey1 = shardRoutingService.calculateRoutingKey(resource);
    var routingKey2 = shardRoutingService.calculateRoutingKey(resource);

    // Then should generate valid and consistent shard ID
    assertNotNull(routingKey1);
    assertEquals(routingKey1, routingKey2);
    var shardId = Integer.parseInt(routingKey1);
    assertTrue(shardId >= 0 && shardId < DEFAULT_NUMBER_OF_SHARDS);
  }

  @Test
  @DisplayName("Should handle null joinField parent gracefully")
  void shouldHandleNullJoinFieldParentGracefully() {
    // Given a document with joinField but null parent
    var identifier = "document-with-null-parent";
    var resource = OBJECT_MAPPER.createObjectNode();
    resource.put(IDENTIFIER_FIELD, identifier);

    var joinField = OBJECT_MAPPER.createObjectNode();
    joinField.put(JOIN_FIELD_NAME, PART_OF);
    joinField.putNull(JOIN_FIELD_PARENT);
    resource.set(JOIN_FIELD, joinField);

    // When calculating routing key
    var routingKey = shardRoutingService.calculateRoutingKey(resource);

    // Then should fall back to identifier
    var expectedRoutingKey =
        String.valueOf(Math.abs(identifier.hashCode()) % DEFAULT_NUMBER_OF_SHARDS);
    assertEquals(expectedRoutingKey, routingKey);
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
    var expectedRoutingKey =
        String.valueOf(Math.abs(identifier.hashCode()) % DEFAULT_NUMBER_OF_SHARDS);
    assertEquals(expectedRoutingKey, routingKey);
  }

  @Test
  @DisplayName("Should handle null identifier gracefully")
  void shouldHandleNullIdentifierGracefully() {
    // Given a document with null identifier
    var resource = OBJECT_MAPPER.createObjectNode();
    resource.putNull(IDENTIFIER_FIELD);

    // When calculating routing key
    var routingKey = shardRoutingService.calculateRoutingKey(resource);

    // Then should generate consistent routing key based on document hash
    assertNotNull(routingKey);
    var shardId = Integer.parseInt(routingKey);
    assertTrue(shardId >= 0 && shardId < DEFAULT_NUMBER_OF_SHARDS);
  }

  @Test
  @DisplayName("Should ensure parent and child documents have same routing key")
  void shouldEnsureParentAndChildHaveSameRoutingKey() {
    // Given parent and child documents
    var parentId = "anthology-123";
    var childId = "chapter-456";

    // Parent document
    var parentResource = OBJECT_MAPPER.createObjectNode();
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

  @ParameterizedTest
  @ValueSource(ints = {1, 3, 5, 7, 10})
  @DisplayName("Should distribute documents across all shards for different shard counts")
  void shouldDistributeDocumentsAcrossAllShards(int numberOfShards) {
    // Given service with different shard count
    var service = new ShardRoutingService(numberOfShards);

    // When generating routing keys for many documents
    var shardCounts = new HashMap<String, Integer>();
    int totalDocuments = 1000;

    for (int i = 0; i < totalDocuments; i++) {
      var resource = OBJECT_MAPPER.createObjectNode();
      resource.put(IDENTIFIER_FIELD, "document-" + i);

      var routingKey = service.calculateRoutingKey(resource);
      shardCounts.merge(routingKey, 1, Integer::sum);
    }

    // Then all shards should have documents
    assertEquals(numberOfShards, shardCounts.size());

    // And distribution should be reasonably even (within 25% of expected)
    var expectedPerShard = totalDocuments / numberOfShards;
    var tolerance = (double) expectedPerShard * 0.25;

    shardCounts.values().forEach(count -> assertEquals(count, expectedPerShard, tolerance));
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
    var parentId = "complex-parent-123";
    var resource = OBJECT_MAPPER.createObjectNode();
    resource.put(IDENTIFIER_FIELD, "child-document");

    var joinField = OBJECT_MAPPER.createObjectNode();
    joinField.put(JOIN_FIELD_NAME, PART_OF);
    joinField.put(JOIN_FIELD_PARENT, parentId);
    joinField.put("someOtherField", "someValue");
    resource.set(JOIN_FIELD, joinField);

    // When calculating routing key
    var routingKey = shardRoutingService.calculateRoutingKey(resource);

    // Then should use parent identifier
    var expectedRoutingKey =
        String.valueOf(Math.abs(parentId.hashCode()) % DEFAULT_NUMBER_OF_SHARDS);
    assertEquals(expectedRoutingKey, routingKey);
  }

  @Test
  @DisplayName("Should handle malformed joinField gracefully")
  void shouldHandleMalformedJoinFieldGracefully() {
    // Given a document with malformed joinField (not an object)
    var identifier = "document-with-malformed-join";
    var resource = OBJECT_MAPPER.createObjectNode();
    resource.put(IDENTIFIER_FIELD, identifier);
    resource.put(JOIN_FIELD, "not-an-object");

    // When calculating routing key
    var routingKey = shardRoutingService.calculateRoutingKey(resource);

    // Then should fall back to identifier
    var expectedRoutingKey =
        String.valueOf(Math.abs(identifier.hashCode()) % DEFAULT_NUMBER_OF_SHARDS);
    assertEquals(expectedRoutingKey, routingKey);
  }

  @Test
  @DisplayName("Should validate shard IDs are within range")
  void shouldValidateShardIdsAreWithinRange() {
    // Given many different identifiers
    var allPossibleShardIds = Set.of("0", "1", "2", "3", "4");

    // When calculating routing keys for many documents
    for (int i = 0; i < 1000; i++) {
      var identifier = "test-document-" + i;
      var routingKey = createResourceAndGetRoutingKey(identifier);

      // Then routing key should be valid shard ID
      assertTrue(
          allPossibleShardIds.contains(routingKey),
          "Routing key " + routingKey + " is not a valid shard ID");
    }
  }

  private ObjectNode createResourceWithJoinField(String parentId) {
    var resource = OBJECT_MAPPER.createObjectNode();
    var joinField = OBJECT_MAPPER.createObjectNode();
    joinField.put(JOIN_FIELD_NAME, ShardRoutingServiceTest.PART_OF);
    joinField.put(JOIN_FIELD_PARENT, parentId);
    resource.set(JOIN_FIELD, joinField);
    return resource;
  }

  private String createResourceAndGetRoutingKey(String identifier) {
    var resource = OBJECT_MAPPER.createObjectNode();
    resource.put(IDENTIFIER_FIELD, identifier);
    return shardRoutingService.calculateRoutingKey(resource);
  }
}
