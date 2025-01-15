package no.unit.nva.search.resource.response;

import com.fasterxml.jackson.databind.JsonNode;
import java.net.URI;

public record Publisher(URI id, String name, String scientificValue) {
  public Publisher(JsonNode publisher) {
    this(
        NodeUtils.toUri(publisher.path("id")),
        publisher.path("name").textValue(),
        publisher.path("scientificValue").textValue());
  }
}
