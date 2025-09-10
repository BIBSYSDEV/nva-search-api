package no.unit.nva.indexingclient;

import static java.util.Objects.isNull;

import com.fasterxml.jackson.databind.JsonNode;
import java.util.Objects;
import nva.commons.core.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Service responsible for calculating routing keys to distribute documents across OpenSearch shards
 * while ensuring parent-child documents are co-located in the same shard.
 *
 * <p>Routing logic priority:
 *
 * <ol>
 *   <li>If document has `joinField.parent`, use parent identifier for routing
 *   <li>If document has `identifier` field, use document identifier for routing
 *   <li>Otherwise, generate random routing key
 * </ol>
 */
public class ShardRoutingService {

  private static final Logger logger = LoggerFactory.getLogger(ShardRoutingService.class);
  private static final String JOIN_FIELD = "joinField";
  private static final String JOIN_FIELD_PARENT = "parent";
  private static final String JOIN_FIELD_NAME = "name";
  private static final String PART_OF = "partOf";
  private static final String IDENTIFIER_FIELD = "identifier";

  private final int numberOfShards;

  /**
   * Creates a new ShardRoutingService with the specified number of shards.
   *
   * @param numberOfShards the total number of shards in the OpenSearch cluster
   * @throws IllegalArgumentException if numberOfShards is less than 1
   */
  public ShardRoutingService(int numberOfShards) {
    var minValidShards = 1;
    if (numberOfShards < minValidShards) {
      throw new IllegalArgumentException(
          "Number of shards must be at least " + minValidShards + ", got: " + numberOfShards);
    }
    this.numberOfShards = numberOfShards;
    logger.debug("Initialized ShardRoutingService with {} shards", numberOfShards);
  }

  /**
   * Calculates the routing key for the given resource document.
   *
   * <p>This method implements the routing logic to ensure:
   *
   * <ul>
   *   <li>Parent-child documents are co-located in the same shard
   *   <li>Documents are distributed evenly across all shards
   *   <li>Consistent routing for the same identifier
   * </ul>
   *
   * @param resource the JSON resource document to calculate routing for
   * @return the routing key as a string representation of shard ID (0 to numberOfShards-1)
   */
  public String calculateRoutingKey(JsonNode resource) {
    Objects.requireNonNull(resource, "Resource cannot be null");

    // 1. Check for parent-child relationship
    var parentIdentifier = extractParentIdentifier(resource);
    if (StringUtils.isNotBlank(parentIdentifier)) {
      int shardId = calculateShardId(parentIdentifier);
      logger.debug(
          "Using parent identifier '{}' for routing, shard: {}", parentIdentifier, shardId);
      return String.valueOf(shardId);
    }

    // 2. Use document identifier
    var documentIdentifier = extractDocumentIdentifier(resource);
    if (StringUtils.isNotBlank(documentIdentifier)) {
      int shardId = calculateShardId(documentIdentifier);
      logger.debug(
          "Using document identifier '{}' for routing, shard: {}", documentIdentifier, shardId);
      return String.valueOf(shardId);
    }

    // 3. Use document string representation as fallback for consistent routing
    var documentString = resource.toString();
    int shardId = calculateShardId(documentString);
    logger.warn(
        "No identifier or parent found in resource, using document hash for routing, shard: {}",
        shardId);
    return String.valueOf(shardId);
  }

  /**
   * Extracts the parent identifier from joinField if present and valid.
   *
   * @param resource the resource document
   * @return the parent identifier, or null if not found or invalid
   */
  private String extractParentIdentifier(JsonNode resource) {
    var joinField = resource.get(JOIN_FIELD);
    if (isNull(joinField) || !joinField.isObject()) {
      return null;
    }

    var joinFieldName = joinField.get(JOIN_FIELD_NAME);
    if (isNull(joinFieldName) || !PART_OF.equalsIgnoreCase(joinFieldName.asText())) {
      return null;
    }

    var parent = joinField.get(JOIN_FIELD_PARENT);
    if (isNull(parent) || parent.isNull()) {
      return null;
    }

    var parentId = parent.asText();
    return StringUtils.isNotBlank(parentId) ? parentId : null;
  }

  /**
   * Extracts the document identifier from the identifier field.
   *
   * @param resource the resource document
   * @return the document identifier, or null if not found or invalid
   */
  private String extractDocumentIdentifier(JsonNode resource) {
    var identifierNode = resource.get(IDENTIFIER_FIELD);
    if (isNull(identifierNode)) {
      return null;
    }

    var identifier = identifierNode.asText();
    return StringUtils.isNotBlank(identifier) ? identifier : null;
  }

  /**
   * Calculates the shard ID for the given identifier using consistent hashing.
   *
   * @param identifier the identifier to hash
   * @return the shard ID (0 to numberOfShards-1)
   */
  private int calculateShardId(String identifier) {
    // Use Math.abs to ensure positive values, handle Integer.MIN_VALUE edge case
    int hashCode = identifier.hashCode();
    int absoluteHash = hashCode == Integer.MIN_VALUE ? 0 : Math.abs(hashCode);
    return absoluteHash % numberOfShards;
  }
}
