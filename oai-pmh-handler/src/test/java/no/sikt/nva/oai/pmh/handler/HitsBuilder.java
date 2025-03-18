package no.sikt.nva.oai.pmh.handler;

import static no.unit.nva.testutils.RandomDataGenerator.randomUri;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.net.URI;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;

final class HitsBuilder {
  private Hit[] hits = new Hit[0];
  private int port = 8080;

  HitsBuilder() {}

  HitsBuilder withPort(int port) {
    this.port = port;
    return this;
  }

  HitsBuilder withHits(Hit... hits) {
    this.hits = Arrays.copyOf(hits, hits.length);
    return this;
  }

  JsonNode build() {
    var hitNodes = Arrays.stream(hits).map(this::generateHitNode).toList();

    return new ArrayNode(JsonNodeFactory.instance, hitNodes);
  }

  private JsonNode generateHitNode(Hit hit) {
    var hitNode = new ObjectNode(JsonNodeFactory.instance);
    hitNode.put("@context", "http://localhost:" + port + "/publication/context");
    hitNode.put("id", "http://localhost/publication/" + hit.identifier());
    hitNode.put("type", "Publication");
    hitNode.put("modifiedDate", ZonedDateTime.now().format(DateTimeFormatter.ISO_OFFSET_DATE_TIME));
    hitNode.set(
        "entityDescription",
        entityDescriptionNode(
            hit.title(), "AcademicArticle", "Journal", randomUri(), randomUri(), randomUri()));

    return hitNode;
  }

  private ObjectNode entityDescriptionNode(
      String title,
      String publicationInstanceType,
      String publicationContextType,
      URI publisher,
      URI series,
      URI journal) {
    var node = new ObjectNode(JsonNodeFactory.instance);
    node.put("mainTitle", title);
    node.put("type", "EntityDescription");
    node.set("publicationDate", publicationDateNode());
    node.set(
        "reference",
        referenceNode(publicationInstanceType, publicationContextType, publisher, series, journal));
    return node;
  }

  private ObjectNode referenceNode(
      String publicationInstanceType,
      String publicationContextType,
      URI publisher,
      URI series,
      URI journal) {
    var node = new ObjectNode(JsonNodeFactory.instance);
    node.put("type", "Reference");
    node.set("publicationInstance", publicationInstanceNode(publicationInstanceType));
    node.set(
        "publicationContext",
        publicationContextNode(publicationContextType, publisher, series, journal));

    return node;
  }

  private ObjectNode publicationInstanceNode(String publicationInstanceType) {
    var node = new ObjectNode(JsonNodeFactory.instance);
    node.put("type", publicationInstanceType);
    return node;
  }

  private ObjectNode publicationContextNode(
      String publicationContextType, URI publisher, URI series, URI journal) {
    var node = new ObjectNode(JsonNodeFactory.instance);
    node.put("type", publicationContextType);
    node.put("id", journal.toString());
    node.put("name", "Journal Name");
    return node;
  }

  private ObjectNode publicationDateNode() {
    var node = new ObjectNode(JsonNodeFactory.instance);
    node.put("type", "PublicationDate");
    node.put("year", "2020");
    node.put("month", "01");
    node.put("day", "01");
    return node;
  }

  record Hit(String identifier, String title) {}
}
