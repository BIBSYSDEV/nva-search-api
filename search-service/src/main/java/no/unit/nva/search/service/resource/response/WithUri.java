package no.unit.nva.search.service.resource.response;

import com.fasterxml.jackson.databind.JsonNode;
import java.net.URI;

public interface WithUri {
  static URI fromNode(JsonNode node) {
    return node.isEmpty() ? null : URI.create(node.asText());
  }
}
