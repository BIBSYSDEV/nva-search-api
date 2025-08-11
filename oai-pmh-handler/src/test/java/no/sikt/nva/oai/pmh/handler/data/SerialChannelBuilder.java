package no.sikt.nva.oai.pmh.handler.data;

import static java.util.Objects.nonNull;

import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.net.URI;

public class SerialChannelBuilder {
  private final String type;
  private final String name;
  private URI id;
  private String printIssn;
  private String onlineIssn;

  public SerialChannelBuilder(String type, String name) {
    this.type = type;
    this.name = name;
  }

  public SerialChannelBuilder withId(URI id) {
    this.id = id;
    return this;
  }

  public SerialChannelBuilder withPrintIssn(String printIssn) {
    this.printIssn = printIssn;
    return this;
  }

  public SerialChannelBuilder withOnlineIssn(String onlineIssn) {
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
