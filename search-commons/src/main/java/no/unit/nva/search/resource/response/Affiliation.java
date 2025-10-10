package no.unit.nva.search.resource.response;

import static no.unit.nva.constants.Words.ID;
import static no.unit.nva.constants.Words.TYPE;

import com.fasterxml.jackson.databind.JsonNode;
import java.net.URI;

public record Affiliation(URI id, String type) {

  public static Affiliation from(JsonNode node) {
    var idNode = NodeUtils.toUri(node.path(ID));
    return new Affiliation(idNode, node.path(TYPE).textValue());
  }
}
