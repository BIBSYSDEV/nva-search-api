package no.unit.nva.indexingclient.utils;

import static java.util.UUID.randomUUID;
import static no.unit.nva.constants.Defaults.objectMapperWithEmpty;

import com.fasterxml.jackson.databind.node.ObjectNode;
import no.unit.nva.indexingclient.ShardRoutingService;

public class ShardRoutingUtils {
  private static final String ID_FIELD = "id";
  private static final String IDENTIFIER_FIELD = "identifier";
  private static final String JOIN_FIELD = "joinField";
  private static final String JOIN_FIELD_PARENT = "parent";
  private static final String JOIN_FIELD_NAME = "name";
  private static final String PART_OF = "partOf";
  private static final String PLACEHOLDER_FIELD = "placeholder";

  public static ObjectNode createExampleDocument() {
    var resource = objectMapperWithEmpty.createObjectNode();
    resource.put(PLACEHOLDER_FIELD, randomUUID().toString());
    return resource;
  }

  public static ObjectNode createStandalonePublicationDocument(String publicationId) {
    var document = createExampleDocument();
    document.put(IDENTIFIER_FIELD, publicationId);
    document.put("type", "Publication");
    document.put("title", "Publication: " + publicationId);
    return document;
  }

  public static ObjectNode createDocumentWithIdField(String idValue) {
    var document = createExampleDocument();
    document.put(ID_FIELD, idValue);
    document.put("type", "Publication");
    document.put("title", "Publication with ID: " + idValue);
    return document;
  }

  public static ObjectNode createDocumentWithBothIdentifierAndId(
      String identifierValue, String idValue) {
    var document = createExampleDocument();
    document.put(IDENTIFIER_FIELD, identifierValue);
    document.put(ID_FIELD, idValue);
    document.put("type", "Publication");
    document.put("title", "Publication with both fields");
    return document;
  }

  public static ObjectNode createResourceWithJoinField(String parentId) {
    var resource = createExampleDocument();
    var joinField = objectMapperWithEmpty.createObjectNode();
    joinField.put(JOIN_FIELD_NAME, PART_OF);
    joinField.put(JOIN_FIELD_PARENT, parentId);
    resource.set(JOIN_FIELD, joinField);
    return resource;
  }

  public static String createResourceAndGetRoutingKey(String identifier) {
    var resource = createExampleDocument();
    resource.put(IDENTIFIER_FIELD, identifier);
    return ShardRoutingService.calculateRoutingKey(resource);
  }

  public static ObjectNode createAnthologyDocument(String anthologyId) {
    var document = createExampleDocument();
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
    var document = createExampleDocument();
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
