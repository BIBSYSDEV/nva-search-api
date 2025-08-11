package no.sikt.nva.oai.pmh.handler.data;

import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;

import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;

public class ResourceDocumentFactory {
  private ResourceDocumentFactory() {}

  private static ObjectNode entityDescription(
      String title,
      URI language,
      ObjectNode referenceNode,
      ObjectNode publicationDateNode,
      ArrayNode contributorsNode) {
    var entityDescriptionNode = JsonNodeFactory.instance.objectNode();
    entityDescriptionNode.put("mainTitle", title);
    if (nonNull(language)) {
      entityDescriptionNode.put("language", language.toString());
    }
    entityDescriptionNode.set("publicationDate", publicationDateNode);
    entityDescriptionNode.set("reference", referenceNode);
    entityDescriptionNode.set("contributors", contributorsNode);
    return entityDescriptionNode;
  }

  public static class ChannelBuilder {
    private final String type;
    private final String name;
    private URI id;
    private String printIssn;
    private String onlineIssn;

    public ChannelBuilder(String type, String name) {
      this.type = type;
      this.name = name;
    }

    public ChannelBuilder withId(URI id) {
      this.id = id;
      return this;
    }

    public ChannelBuilder withPrintIssn(String printIssn) {
      this.printIssn = printIssn;
      return this;
    }

    public ChannelBuilder withOnlineIssn(String onlineIssn) {
      this.onlineIssn = onlineIssn;
      return this;
    }

    public ObjectNode build() {
      var channelNode = JsonNodeFactory.instance.objectNode();
      channelNode.put("name", name);
      if (nonNull(id)) {
        channelNode.put("id", id.toString());
      }
      if (nonNull(type)) {
        channelNode.put("type", type);
      }
      if (nonNull(printIssn)) {
        channelNode.put("printIssn", printIssn);
      }
      if (nonNull(onlineIssn)) {
        channelNode.put("onlineIssn", onlineIssn);
      }
      return channelNode;
    }
  }

  public static ResourceDocumentBuilder builder(
      URI id,
      String title,
      String publicationYear,
      String publicationMonth,
      String publicationDay) {
    return new ResourceDocumentBuilder(
        id, title, publicationYear, publicationMonth, publicationDay);
  }

  public static class ResourceDocumentBuilder {
    private final URI id;
    private final String title;
    private final String publicationYear;
    private final String publicationMonth;
    private final String publicationDay;
    private URI doi;
    private final List<ObjectNode> contributorNodes = new ArrayList<>();
    private final List<ObjectNode> additionalIdentifierNodes = new ArrayList<>();
    private ObjectNode referenceNode;
    private URI language;

    private ResourceDocumentBuilder(
        URI id,
        String title,
        String publicationYear,
        String publicationMonth,
        String publicationDay) {
      this.id = id;
      this.title = title;
      if (isNull(publicationYear)) {
        throw new IllegalStateException("Publication year cannot be null");
      }
      this.publicationYear = publicationYear;
      this.publicationMonth = publicationMonth;
      this.publicationDay = publicationDay;
    }

    public ResourceDocumentBuilder withDoi(URI doi) {
      this.doi = doi;
      return this;
    }

    public ResourceDocumentBuilder withContributor(String name, URI id) {
      var identityNode = JsonNodeFactory.instance.objectNode();
      identityNode.put("name", name);
      if (nonNull(id)) {
        identityNode.put("id", id.toString());
      }
      var contributorNode = JsonNodeFactory.instance.objectNode();
      contributorNode.set("identity", identityNode);
      this.contributorNodes.add(contributorNode);
      return this;
    }

    public ResourceDocumentBuilder withAdditionalIdentifier(String type, String value) {
      var additionalIdentifierNode = JsonNodeFactory.instance.objectNode();
      additionalIdentifierNode.put("type", type);
      additionalIdentifierNode.put("value", value);
      this.additionalIdentifierNodes.add(additionalIdentifierNode);
      return this;
    }

    public ResourceDocumentBuilder withLanguage(URI language) {
      this.language = language;
      return this;
    }

    void applyType(ObjectNode referenceNode) {
      this.referenceNode = referenceNode;
    }

    public ObjectNode build() {
      if (isNull(referenceNode)) {
        throw new IllegalStateException("Publication reference not populated");
      }

      var publicationDateNode = JsonNodeFactory.instance.objectNode();
      publicationDateNode.put("year", publicationYear);
      if (nonNull(publicationMonth)) {
        publicationDateNode.put("month", publicationMonth);
      }
      if (nonNull(publicationDay)) {
        publicationDateNode.put("day", publicationDay);
      }

      var contributorsNode = JsonNodeFactory.instance.arrayNode();
      contributorsNode.addAll(contributorNodes);

      var entityDescriptionNode =
          entityDescription(title, language, referenceNode, publicationDateNode, contributorsNode);

      var additionalIdentifiersNode = JsonNodeFactory.instance.arrayNode();
      additionalIdentifiersNode.addAll(additionalIdentifierNodes);

      var resourceRoot = JsonNodeFactory.instance.objectNode();
      resourceRoot.put("id", id.toString());
      resourceRoot.set("entityDescription", entityDescriptionNode);
      if (nonNull(doi)) {
        resourceRoot.put("doi", doi.toString());
      }
      resourceRoot.put("modifiedDate", "2023-01-01T01:02:03.123456789Z");

      resourceRoot.set("additionalIdentifiers", additionalIdentifiersNode);

      return resourceRoot;
    }

    public AcademicArticleBuilder academicArticle(ChannelBuilder journalBuilder) {
      return new AcademicArticleBuilder(this, journalBuilder);
    }
  }
}
