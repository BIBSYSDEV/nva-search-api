package no.unit.nva.search.resource.response;

import static no.unit.nva.constants.Words.ID;
import static no.unit.nva.constants.Words.TYPE;

import com.fasterxml.jackson.databind.JsonNode;

public record Affiliation(String id, String type) {

  public Affiliation(JsonNode node) {
    this(node.path(ID).textValue(), node.path(TYPE).textValue());
  }
}
