package no.unit.nva.search.resource.response;

import com.fasterxml.jackson.databind.JsonNode;
import java.net.URI;
import java.util.Objects;
import no.unit.nva.search.resource.response.utils.IdExtractor;

public final class NodeUtils {

  private NodeUtils() {}

  public static URI toUri(JsonNode node) {
    return Objects.isNull(node) || !node.isTextual() ? null : IdExtractor.from(node).orElse(null);
  }
}
