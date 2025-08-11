package no.sikt.nva.oai.pmh.handler.data;

import static java.util.Objects.nonNull;

import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.net.URI;

public class PublisherChannelBuilder {
  private final String type = "Publisher";
  private final String name;
  private URI id;

  public PublisherChannelBuilder(String name) {
    this.name = name;
  }

  public PublisherChannelBuilder withId(URI id) {
    this.id = id;
    return this;
  }

  public ObjectNode build() {
    var channelNode = JsonNodeFactory.instance.objectNode();
    channelNode.put("name", name);
    channelNode.put("type", type);
    if (nonNull(id)) {
      channelNode.put("id", id.toString());
    }
    return channelNode;
  }
}
