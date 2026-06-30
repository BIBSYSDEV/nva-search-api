package no.unit.nva.search.common.bibliography;

import com.fasterxml.jackson.databind.JsonNode;
import java.util.Optional;

final class SchemaOrgNodeReader {

  private SchemaOrgNodeReader() {} // NO-OP

  static Optional<String> text(JsonNode node, String pointer) {
    var value = node.at(pointer);
    if (value.isMissingNode() || value.isNull()) {
      return Optional.empty();
    }
    var text = value.asText();
    return text.isBlank() ? Optional.empty() : Optional.of(text);
  }
}
