package no.sikt.nva.oai.pmh.handler.data;

import static java.util.Objects.nonNull;

import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import no.sikt.nva.oai.pmh.handler.data.ResourceDocumentFactory.ChannelBuilder;
import no.sikt.nva.oai.pmh.handler.data.ResourceDocumentFactory.ResourceDocumentBuilder;

public class AcademicArticleBuilder extends AbstractReferenceBuilder<AcademicArticleBuilder> {
  private final ResourceDocumentBuilder resourceDocumentBuilder;
  private final ChannelBuilder journalBuilder;

  AcademicArticleBuilder(
      ResourceDocumentBuilder resourceDocumentBuilder, ChannelBuilder journalBuilder) {
    this.resourceDocumentBuilder = resourceDocumentBuilder;
    this.journalBuilder = journalBuilder;
  }

  public ResourceDocumentBuilder apply() {
    var publicationInstanceTypeNode = JsonNodeFactory.instance.objectNode();
    publicationInstanceTypeNode.put("type", "AcademicArticle");
    var publicationContextTypeNode = journalBuilder.build();
    var referenceNode = JsonNodeFactory.instance.objectNode();
    referenceNode.set("publicationInstance", publicationInstanceTypeNode);
    referenceNode.set("publicationContext", publicationContextTypeNode);
    if (nonNull(referenceDoi)) {
      referenceNode.put("doi", referenceDoi.toString());
    }
    this.resourceDocumentBuilder.applyType(referenceNode);
    return resourceDocumentBuilder;
  }
}
