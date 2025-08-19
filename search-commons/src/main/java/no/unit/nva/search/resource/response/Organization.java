package no.unit.nva.search.resource.response;

import static nva.commons.core.attempt.Try.attempt;
import com.fasterxml.jackson.databind.JsonNode;
import java.net.URI;
import java.util.Map;
import no.unit.nva.commons.json.JsonUtils;

public record Organization(URI id, Map<String, String> labels) {

  public static Organization fromJsonNode(JsonNode node) {
    return attempt(() -> JsonUtils.dtoObjectMapper.treeToValue(node, Organization.class)).orElseThrow();
  }
}
