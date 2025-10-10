package no.unit.nva.search.resource.response.utils;

import com.fasterxml.jackson.databind.JsonNode;
import java.net.URI;
import java.util.Optional;

public final class IdExtractor {
  private IdExtractor() {
    // NO-OP
  }

  /**
   * This method exists solely because people refuse to accept that we are dealing with graphs in
   * JSON-LD rather than plain old JSON, so we now need to fix the id nodes to allow both IRIs and
   * blank nodes.
   *
   * @param node A node containing the id.
   * @return A string containing a URI or null.
   */
  public static Optional<URI> from(JsonNode node) {
    try {
      return Optional.of(URI.create(node.textValue()));
    } catch (IllegalArgumentException | NullPointerException e) {
      return Optional.empty();
    }
  }
}
