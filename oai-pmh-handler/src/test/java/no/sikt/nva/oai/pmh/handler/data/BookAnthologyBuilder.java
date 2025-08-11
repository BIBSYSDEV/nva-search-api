package no.sikt.nva.oai.pmh.handler.data;

import static java.util.Objects.nonNull;

import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import java.util.HashSet;
import java.util.Set;
import no.sikt.nva.oai.pmh.handler.data.ResourceDocumentFactory.ResourceDocumentBuilder;

public class BookAnthologyBuilder extends AbstractReferenceBuilder<BookAnthologyBuilder> {
  private final ResourceDocumentBuilder resourceDocumentBuilder;
  private final PublisherChannelBuilder publisherBuilder;
  private final Set<String> isbnList = new HashSet<>();
  private SerialChannelBuilder seriesBuilder;

  BookAnthologyBuilder(
      ResourceDocumentBuilder resourceDocumentBuilder, PublisherChannelBuilder publisherBuilder) {
    this.resourceDocumentBuilder = resourceDocumentBuilder;
    this.publisherBuilder = publisherBuilder;
  }

  public BookAnthologyBuilder withIsbn(String isbn) {
    isbnList.add(isbn);
    return this;
  }

  public BookAnthologyBuilder withSeries(SerialChannelBuilder seriesBuilder) {
    this.seriesBuilder = seriesBuilder;
    return this;
  }

  public ResourceDocumentBuilder apply() {
    var publicationInstanceNode = JsonNodeFactory.instance.objectNode();
    publicationInstanceNode.put("type", "ReportBasic");
    var publicationContextNode = JsonNodeFactory.instance.objectNode();
    publicationContextNode.put("type", "Report");
    publicationContextNode.set("publisher", publisherBuilder.build());
    if (nonNull(seriesBuilder)) {
      publicationContextNode.set("series", seriesBuilder.build());
    }
    if (!isbnList.isEmpty()) {
      var isbnListNode = JsonNodeFactory.instance.arrayNode();
      isbnList.forEach(isbnListNode::add);
      publicationContextNode.set("isbnList", isbnListNode);
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
