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
  public static final String PLACEHOLDER_FIELD = "placeholder";

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
    document.put("type", "Publication");
    document.put("title", "Publication: " + identifier);
    return document;
  }

  public static ObjectNode createDocumentWithIdField(String idValue) {
    var document = createDocumentNode();
    document.put(ID_FIELD, idValue);
    document.put("type", "Publication");
    document.put("title", "Publication with ID: " + idValue);
    return document;
  }

  public static ObjectNode createDocumentWithBothIdentifierAndId(
      String identifierValue, String idValue) {
    var document = createDocumentNode();
    document.put(IDENTIFIER_FIELD, identifierValue);
    document.put(ID_FIELD, idValue);
    document.put("type", "Publication");
    document.put("title", "Publication with both fields");
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

  public static ObjectNode createAnthologyDocument(String anthologyId) {
    var document = createDocumentNode();
    document.put("identifier", anthologyId);
    document.put("type", "Anthology");
    document.put("title", "Anthology: " + anthologyId);

    // Add joinField for parent document
    var joinField = objectMapperWithEmpty.createObjectNode();
    joinField.put("name", "hasParts");
    document.set("joinField", joinField);

    return document;
  }

  public static ObjectNode createChapterDocument(String chapterId, String parentAnthologyId) {
    var document = createDocumentNode();
    document.put("identifier", chapterId);
    document.put("type", "Chapter");
    document.put("title", "Chapter: " + chapterId);

    // Add joinField to indicate parent-child relationship
    var joinField = objectMapperWithEmpty.createObjectNode();
    joinField.put("name", "partOf");
    joinField.put("parent", parentAnthologyId);
    document.set("joinField", joinField);

    return document;
  }
}
