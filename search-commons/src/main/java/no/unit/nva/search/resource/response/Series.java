package no.unit.nva.search.resource.response;

import static no.unit.nva.constants.Words.ID;
import static no.unit.nva.constants.Words.NAME;
import static no.unit.nva.constants.Words.SCIENTIFIC_VALUE;

import com.fasterxml.jackson.databind.JsonNode;
import java.net.URI;

public record Series(URI id, String name, String scientificValue) {
  public Series(JsonNode series) {
    this(
        NodeUtils.toUri(series.path(ID)),
        series.path(NAME).textValue(),
        series.path(SCIENTIFIC_VALUE).textValue());
  }
}
