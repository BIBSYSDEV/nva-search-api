package no.sikt.nva.oai.pmh.handler;

import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;

import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.util.Arrays;

public class HitBuilder {

  private final ObjectNode referenceNode;
  private final int port;
  private String identifier;
  private String title;
  private String[] contributors = new String[] {};
  private ObjectNode publicationDateNode;
  private boolean publicationDatePresent = true;

  private HitBuilder(int port, ObjectNode referenceNode) {
    this.port = port;
    this.referenceNode = referenceNode;
  }

  public static HitBuilder academicArticle(int port, String journalName) {
    var referenceNode = academicArticleReferenceNode(journalName);
    return new HitBuilder(port, referenceNode);
  }

  public static HitBuilder academicArticleWithMissingJournalInformation(int port) {
    var referenceNode = academicArticleReferenceNode(null);
    return new HitBuilder(port, referenceNode);
  }

  public static HitBuilder reportBasicWithMissingChannelName(int port) {
    var referenceNode = reportBasicReferenceNode(null, null);
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

  public HitBuilder withEmptyPublicationDate() {
    publicationDateNode = new ObjectNode(JsonNodeFactory.instance);
    return this;
  }

  public HitBuilder withNoPublicationDate() {
    this.publicationDatePresent = false;
    return this;
  }

  public ObjectNode build() {
    var rootNode = new ObjectNode(JsonNodeFactory.instance);
    rootNode.put("type", "Publication");
    rootNode.put("@context", "https://localhost:" + port + "/publication/context");
    rootNode.put("id", "https://localhost/publication/" + this.identifier);
    rootNode.put("identifier", this.identifier);
    var publicationDateNodeToUse = resolvePublicationDateNode();
    var contributorsPreviewNode = new ArrayNode(JsonNodeFactory.instance);
    Arrays.stream(contributors)
        .forEach(contributor -> contributorsPreviewNode.add(contributorNode(contributor)));
    rootNode.set(
        "entityDescription",
        entityDescriptionNode(
            title, referenceNode, publicationDateNodeToUse, contributorsPreviewNode));
    rootNode.put("modifiedDate", "2023-01-01T01:02:03.123456789Z");
    return rootNode;
  }

  private ObjectNode resolvePublicationDateNode() {
    ObjectNode publicationDateNodeToUse;
    if (publicationDatePresent && isNull(publicationDateNode)) {
      publicationDateNodeToUse = new ObjectNode(JsonNodeFactory.instance);
      publicationDateNodeToUse.put("year", "2020");
      publicationDateNodeToUse.put("month", "01");
      publicationDateNodeToUse.put("day", "01");
    } else if (publicationDatePresent) {
      publicationDateNodeToUse = publicationDateNode;
    } else {
      publicationDateNodeToUse = null;
    }
    return publicationDateNodeToUse;
  }

  private static ObjectNode contributorNode(String contributor) {
    var contributorNode = new ObjectNode(JsonNodeFactory.instance);
    contributorNode.set("identity", identityNode(contributor));
    return contributorNode;
  }

  private static ObjectNode identityNode(String contributor) {
    var identityNode = new ObjectNode(JsonNodeFactory.instance);
    identityNode.put("name", contributor);
    return identityNode;
  }

  private static ObjectNode entityDescriptionNode(
      String title,
      ObjectNode referenceNode,
      ObjectNode publicationDateNode,
      ArrayNode contributorsPreviewNode) {
    var node = new ObjectNode(JsonNodeFactory.instance);
    node.set("reference", referenceNode);
    if (publicationDateNode != null) {
      node.set("publicationDate", publicationDateNode);
    }
    node.put("mainTitle", title);
    node.set("contributorsPreview", contributorsPreviewNode);
    return node;
  }

  private static ObjectNode academicArticleReferenceNode(String journalName) {
    var publicationInstance = new ObjectNode(JsonNodeFactory.instance);
    publicationInstance.put("type", "AcademicArticle");
    var publicationContext = new ObjectNode(JsonNodeFactory.instance);
    publicationContext.put("type", "Journal");
    if (nonNull(journalName)) {
      publicationContext.put("name", journalName);
    }

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
    if (nonNull(publisherName)) {
      node.put("name", publisherName);
    }
    return node;
  }

  private static ObjectNode seriesNode(String seriesName) {
    var node = new ObjectNode(JsonNodeFactory.instance);
    if (nonNull(seriesName)) {
      node.put("name", seriesName);
    }
    return node;
  }
}
