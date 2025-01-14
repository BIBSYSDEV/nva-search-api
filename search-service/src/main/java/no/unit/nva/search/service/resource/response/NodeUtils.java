package no.unit.nva.search.service.resource.response;

import static java.util.Objects.isNull;

import com.fasterxml.jackson.databind.JsonNode;
import java.net.URI;

public interface NodeUtils {
  static URI toUri(JsonNode node) {
    return isNull(node) || node.isEmpty() ? null : URI.create(node.asText());
  }
}
