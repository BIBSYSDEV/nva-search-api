package no.sikt.nva.oai.pmh.handler;

import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;

public class HitBuilder {
  private final ObjectNode referenceNode;
  private final int port;
  private String identifier;
  private String title;
  private String[] contributors = new String[] {};

  private HitBuilder(int port, ObjectNode referenceNode) {
    this.port = port;
    this.referenceNode = referenceNode;
  }

  public static HitBuilder academicArticle(int port, String journalName) {
    var referenceNode = academicArticleReferenceNode(journalName);
    return new HitBuilder(port, referenceNode);
  }

  public static HitBuilder reportBasic(int port, String publisherName, String seriesName) {
    var referenceNode = reportBasicReferenceNode(publisherName, seriesName);
    return new HitBuilder(port, referenceNode);
  }

  public HitBuilder withIdentifier(String identifier) {
    this.identifier = identifier;
    return this;
  }

  public HitBuilder withTitle(String title) {
    this.title = title;
    return this;
  }

  public HitBuilder withContributors(String... contributors) {
    this.contributors = contributors;
    return this;
  }

  public ObjectNode build() {
    var rootNode = new ObjectNode(JsonNodeFactory.instance);
    rootNode.put("type", "Publication");
    rootNode.put("@context", "http://localhost:" + port + "/publication/context");
    rootNode.put("id", "http://localhost/publication/" + this.identifier);
    rootNode.put("identifier", this.identifier);
    var publicationDateNode = new ObjectNode(JsonNodeFactory.instance);
    publicationDateNode.put("year", "2020");
    publicationDateNode.put("month", "01");
    publicationDateNode.put("day", "01");
    var contributorsPreviewNode = new ArrayNode(JsonNodeFactory.instance);
    Arrays.stream(contributors)
        .forEach(contributor -> contributorsPreviewNode.add(contributorNode(contributor)));
    rootNode.set(
        "entityDescription",
        entityDescriptionNode(title, referenceNode, publicationDateNode, contributorsPreviewNode));
    rootNode.put(
        "modifiedDate", ZonedDateTime.now().format(DateTimeFormatter.ISO_OFFSET_DATE_TIME));
    return rootNode;
  }

  private ObjectNode contributorNode(String contributor) {
    var contributorNode = new ObjectNode(JsonNodeFactory.instance);
    contributorNode.set("identity", identityNode(contributor));
    return contributorNode;
  }

  private ObjectNode identityNode(String contributor) {
    var identityNode = new ObjectNode(JsonNodeFactory.instance);
    identityNode.put("name", contributor);
    return identityNode;
  }

  private ObjectNode entityDescriptionNode(
      String title,
      ObjectNode referenceNode,
      ObjectNode publicationDateNode,
      ArrayNode contributorsPreviewNode) {
    var node = new ObjectNode(JsonNodeFactory.instance);
    node.set("reference", referenceNode);
    node.set("publicationDate", publicationDateNode);
    node.put("mainTitle", title);
    node.set("contributorsPreview", contributorsPreviewNode);
    return node;
  }

  private static ObjectNode academicArticleReferenceNode(String journalName) {
    var publicationInstance = new ObjectNode(JsonNodeFactory.instance);
    publicationInstance.put("type", "AcademicArticle");
    var publicationContext = new ObjectNode(JsonNodeFactory.instance);
    publicationContext.put("type", "Journal");
    publicationContext.put("name", journalName);

    var referenceNode = new ObjectNode(JsonNodeFactory.instance);
    referenceNode.put("type", "Reference");
    referenceNode.set("publicationInstance", publicationInstance);
    referenceNode.set("publicationContext", publicationContext);

    return referenceNode;
  }

  private static ObjectNode reportBasicReferenceNode(String publisherName, String seriesName) {
    var publicationInstance = new ObjectNode(JsonNodeFactory.instance);
    publicationInstance.put("type", "ReportBasic");
    var publicationContext = new ObjectNode(JsonNodeFactory.instance);
    publicationContext.put("type", "Report");
    publicationContext.set("publisher", publisherNode(publisherName));
    publicationContext.set("series", seriesNode(seriesName));

    var referenceNode = new ObjectNode(JsonNodeFactory.instance);
    referenceNode.put("type", "Reference");
    referenceNode.set("publicationInstance", publicationInstance);
    referenceNode.set("publicationContext", publicationContext);

    return referenceNode;
  }

  private static ObjectNode publisherNode(String publisherName) {
    var node = new ObjectNode(JsonNodeFactory.instance);
    node.put("name", publisherName);
    return node;
  }

  private static ObjectNode seriesNode(String seriesName) {
    var node = new ObjectNode(JsonNodeFactory.instance);
    node.put("name", seriesName);
    return node;
  }
}
