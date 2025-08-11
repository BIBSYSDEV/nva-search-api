package no.sikt.nva.oai.pmh.handler.data;

import static java.util.Objects.nonNull;

import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import no.sikt.nva.oai.pmh.handler.data.ResourceDocumentFactory.ResourceDocumentBuilder;

public class ReportBasicBuilder extends AbstractReferenceBuilder<ReportBasicBuilder> {
  private final ResourceDocumentBuilder resourceDocumentBuilder;
  private final PublisherChannelBuilder publisherBuilder;
  private final SerialChannelBuilder seriesBuilder;

  ReportBasicBuilder(
      ResourceDocumentBuilder resourceDocumentBuilder,
      PublisherChannelBuilder publisherBuilder,
      SerialChannelBuilder seriesBuilder) {
    this.resourceDocumentBuilder = resourceDocumentBuilder;
    this.publisherBuilder = publisherBuilder;
    this.seriesBuilder = seriesBuilder;
  }

  public ResourceDocumentBuilder apply() {
    var publicationInstanceNode = JsonNodeFactory.instance.objectNode();
    publicationInstanceNode.put("type", "ReportBasic");
    var publicationContextNode = JsonNodeFactory.instance.objectNode();
    publicationContextNode.put("type", "Report");
    if (nonNull(publisherBuilder)) {
      publicationContextNode.set("publisher", publisherBuilder.build());
    }
    if (nonNull(seriesBuilder)) {
      publicationContextNode.set("series", seriesBuilder.build());
    }
    var referenceNode = JsonNodeFactory.instance.objectNode();
    referenceNode.set("publicationInstance", publicationInstanceNode);
    referenceNode.set("publicationContext", publicationContextNode);
    if (nonNull(referenceDoi)) {
      referenceNode.put("doi", referenceDoi.toString());
    }
    this.resourceDocumentBuilder.applyType(referenceNode);
    return resourceDocumentBuilder;
  }
}
