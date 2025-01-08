package no.unit.nva.search.service.resource.response;

import static no.unit.nva.search.model.constant.Words.ID;
import static no.unit.nva.search.model.constant.Words.NAME;
import static no.unit.nva.search.model.constant.Words.SCIENTIFIC_VALUE;

import com.fasterxml.jackson.databind.JsonNode;

import java.net.URI;

public record PublishingDetails(
    URI id, String type, String name, URI doi, ScientificValue series, ScientificValue publisher)
    implements WithUri {

  public PublishingDetails(
      JsonNode id,
      JsonNode type,
      JsonNode name,
      JsonNode doi,
      JsonNode series,
      JsonNode publisher) {
    this(
        WithUri.fromNode(id),
        type.asText(),
        name.asText(),
        WithUri.fromNode(doi),
        new ScientificValue(series.get(ID), series.get(NAME), series.get(SCIENTIFIC_VALUE)),
        new ScientificValue(
            publisher.get(ID), publisher.get(NAME), publisher.get(SCIENTIFIC_VALUE)));
  }

  public record ScientificValue(URI id, String name, String scientificValue) {

    public ScientificValue(JsonNode id, JsonNode name, JsonNode scientificValue) {
      this(WithUri.fromNode(id), name.asText(), scientificValue.asText());
    }
  }
}
