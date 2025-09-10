package no.unit.nva.indexingclient.utils;

import static java.util.UUID.randomUUID;
import static no.unit.nva.constants.Defaults.objectMapperWithEmpty;

import com.fasterxml.jackson.databind.node.ObjectNode;
import no.unit.nva.identifiers.SortableIdentifier;
import no.unit.nva.indexingclient.ShardRoutingService;
import no.unit.nva.indexingclient.models.EventConsumptionAttributes;
import no.unit.nva.indexingclient.models.IndexDocument;

public class ShardRoutingUtils {
  public static final String ID_FIELD = "id";
  public static final String IDENTIFIER_FIELD = "identifier";
  public static final String JOIN_FIELD = "joinField";
  public static final String JOIN_FIELD_PARENT = "parent";
  public static final String JOIN_FIELD_NAME = "name";
  public static final String PART_OF = "partOf";
  public static final String HAS_PARTS = "hasParts";
  public static final String PLACEHOLDER_FIELD = "placeholder";
  public static final String PLACEHOLDER = "PARENT_IDENTIFIER_NOT_FOUND";
  private static final String TYPE_FIELD = "type";
  private static final String TITLE_FIELD = "title";

  public static ObjectNode createDocumentNode() {
    var resource = objectMapperWithEmpty.createObjectNode();
    resource.put(PLACEHOLDER_FIELD, randomUUID().toString());
    return resource;
  }

  public static IndexDocument toIndexDocument(ObjectNode document, String indexName) {
    return new IndexDocument(
        new EventConsumptionAttributes(indexName, SortableIdentifier.next()), document);
  }

  public static ObjectNode createStandalonePublicationDocument(String identifier) {
    var document = createDocumentNode();
    document.put(IDENTIFIER_FIELD, identifier);
    document.put(TYPE_FIELD, "Publication");
    document.put(TITLE_FIELD, "Publication: " + identifier);
    document.set(JOIN_FIELD, createJoinFieldForPlaceholderValue());
    return document;
  }

  public static ObjectNode createAnthologyDocument(String anthologyId) {
    var document = createDocumentNode();
    document.put(IDENTIFIER_FIELD, anthologyId);
    document.put(TYPE_FIELD, "Anthology");
    document.put(TITLE_FIELD, "Anthology: " + anthologyId);
    document.set(JOIN_FIELD, createJoinFieldForParent());
    return document;
  }

  public static ObjectNode createChapterDocument(String chapterId, String parentAnthologyId) {
    var document = createDocumentNode();
    document.put(IDENTIFIER_FIELD, chapterId);
    document.put(TYPE_FIELD, "Chapter");
    document.put(TITLE_FIELD, "Chapter: " + chapterId);
    document.set(JOIN_FIELD, createJoinFieldForChild(parentAnthologyId));
    return document;
  }

  public static ObjectNode createDocumentWithIdentifierField(String idValue) {
    var document = createDocumentNode();
    document.put(IDENTIFIER_FIELD, idValue);
    document.put(TYPE_FIELD, "Publication");
    document.put(TITLE_FIELD, "Publication with ID: " + idValue);
    return document;
  }

  public static ObjectNode createDocumentWithIdField(String idValue) {
    var document = createDocumentNode();
    document.put(ID_FIELD, idValue);
    document.put(TYPE_FIELD, "Publication");
    document.put(TITLE_FIELD, "Publication with ID: " + idValue);
    return document;
  }

  public static ObjectNode createDocumentWithBothIdentifierAndId(
      String identifierValue, String idValue) {
    var document = createDocumentNode();
    document.put(IDENTIFIER_FIELD, identifierValue);
    document.put(ID_FIELD, idValue);
    document.put(TYPE_FIELD, "Publication");
    document.put(TITLE_FIELD, "Publication with both fields");
    return document;
  }

  public static ObjectNode createResourceWithJoinField(String parentId) {
    var resource = createDocumentNode();
    var joinField = objectMapperWithEmpty.createObjectNode();
    joinField.put(JOIN_FIELD_NAME, PART_OF);
    joinField.put(JOIN_FIELD_PARENT, parentId);
    resource.set(JOIN_FIELD, joinField);
    return resource;
  }

  public static String createResourceAndGetRoutingKey(String identifier) {
    var resource = createDocumentNode();
    resource.put(IDENTIFIER_FIELD, identifier);
    return ShardRoutingService.calculateRoutingKey(resource);
  }

  public static ObjectNode createJoinFieldForParent() {
    var joinField = objectMapperWithEmpty.createObjectNode();
    joinField.put(JOIN_FIELD_NAME, HAS_PARTS);
    joinField.putNull(JOIN_FIELD_PARENT);

    return joinField;
  }

  public static ObjectNode createJoinFieldForChild(String parentIdentifier) {
    var joinField = objectMapperWithEmpty.createObjectNode();
    joinField.put(JOIN_FIELD_NAME, PART_OF);
    joinField.put(JOIN_FIELD_PARENT, parentIdentifier);

    return joinField;
  }

  public static ObjectNode createJoinFieldForPlaceholderValue() {
    var joinField = objectMapperWithEmpty.createObjectNode();
    joinField.put(JOIN_FIELD_NAME, HAS_PARTS);
    joinField.put(JOIN_FIELD_PARENT, PLACEHOLDER);

    return joinField;
  }
}
