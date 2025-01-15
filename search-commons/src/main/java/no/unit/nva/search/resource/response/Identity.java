package no.unit.nva.search.resource.response;

import static no.unit.nva.constants.Words.ID;
import static no.unit.nva.constants.Words.NAME;
import static no.unit.nva.constants.Words.ORC_ID;

import com.fasterxml.jackson.databind.JsonNode;
import java.net.URI;

public record Identity(URI id, String name, URI orcId) {

  public Identity(JsonNode identity) {
    this(
        NodeUtils.toUri(identity.path(ID)),
        identity.path(NAME).textValue(),
        NodeUtils.toUri(identity.path(ORC_ID)));
  }
}
