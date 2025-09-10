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
 */
public class ShardRoutingService {

  private static final Logger logger = LoggerFactory.getLogger(ShardRoutingService.class);
  private static final String JOIN_FIELD = "joinField";
  private static final String JOIN_FIELD_PARENT = "parent";
  private static final String JOIN_FIELD_NAME = "name";
  private static final String PART_OF = "partOf";
  private static final String IDENTIFIER_FIELD = "identifier";
  private static final String ID_FIELD = "id";

  public ShardRoutingService() {}

  /**
   * Calculates the routing key for the given resource document.
   *
   * <p>Routing priority order:
   *
   * <ol>
   *   <li>Parent identifier from joinField (for parent-child co-location)
   *   <li>Document identifier field
   *   <li>Document id field
   *   <li>Full document string (fallback)
   * </ol>
   *
   * @param resource the JSON resource document to calculate routing for
   * @return the string to be used as routing key (OpenSearch will hash this to determine shard)
   */
  public static String calculateRoutingKey(JsonNode resource) {
    Objects.requireNonNull(resource, "Resource cannot be null");

    var parentIdentifier = extractParentIdentifier(resource);
    if (StringUtils.isNotBlank(parentIdentifier)) {
      logger.info("Using parent identifier '{}' for routing", parentIdentifier);
      return parentIdentifier;
    }

    var docIdentifier = extractFieldValue(resource, IDENTIFIER_FIELD);
    if (StringUtils.isNotBlank(docIdentifier)) {
      logger.info("Using document identifier '{}' for routing", docIdentifier);
      return docIdentifier;
    }

    var docId = extractFieldValue(resource, ID_FIELD);
    if (StringUtils.isNotBlank(docId)) {
      logger.debug("Using document id '{}' for routing", docId);
      return docId;
    }

    logger.warn(
        "No identifier, id, or parent found in resource, using document string for routing");
    return resource.toString();
  }

  /**
   * Extracts a field value from the resource document.
   *
   * @param resource the resource document
   * @param fieldName the field name to extract
   * @return the field value, or null if not found or blank
   */
  private static String extractFieldValue(JsonNode resource, String fieldName) {
    var fieldNode = resource.get(fieldName);
    if (isNull(fieldNode) || fieldNode.isNull()) {
      return null;
    }
    return fieldNode.asText();
  }

  /**
   * Extracts the parent identifier from joinField if present and valid.
   *
   * @param resource the resource document
   * @return the parent identifier, or null if not found or invalid
   */
  private static String extractParentIdentifier(JsonNode resource) {
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
}
