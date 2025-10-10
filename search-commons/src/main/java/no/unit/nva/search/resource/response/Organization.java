package no.unit.nva.search.resource.response;

import static java.util.Objects.nonNull;
import static no.unit.nva.constants.Words.ID;
import static no.unit.nva.constants.Words.LABELS;
import static nva.commons.core.attempt.Try.attempt;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import java.net.URI;
import java.util.Collections;
import java.util.Map;
import no.unit.nva.commons.json.JsonUtils;

public record Organization(URI id, Map<String, String> labels) {

  public Organization(URI id, Map<String, String> labels) {
    this.id = id;
    this.labels = nonNull(labels) ? labels : Collections.emptyMap();
  }

  public static Organization fromJsonNode(JsonNode node) {
    var id = NodeUtils.toUri(node.get(ID));
    var type = new TypeReference<Map<String, String>>() {};
    var labels =
        attempt(() -> JsonUtils.dtoObjectMapper.treeToValue(node.get(LABELS), type)).orElseThrow();
    return new Organization(id, labels);
  }
}
