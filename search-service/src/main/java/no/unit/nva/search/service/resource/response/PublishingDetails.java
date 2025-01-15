package no.unit.nva.search.service.resource.response;

import static java.util.Objects.isNull;
import static no.unit.nva.search.model.constant.Words.ID;
import static no.unit.nva.search.model.constant.Words.NAME;
import static no.unit.nva.search.model.constant.Words.SCIENTIFIC_VALUE;

import com.fasterxml.jackson.databind.JsonNode;
import java.net.URI;

public record PublishingDetails(
    URI id, String type, String name, URI doi, ScientificValue series, ScientificValue publisher)
    implements NodeUtils {

  public PublishingDetails(
      JsonNode id,
      JsonNode type,
      JsonNode name,
      JsonNode doi,
      JsonNode series,
      JsonNode publisher) {
    this(
        NodeUtils.toUri(id),
        isNull(type) ? null : type.asText(),
        isNull(name) ? null : name.asText(),
        NodeUtils.toUri(doi),
        isNull(series) ? null : new ScientificValue(series),
        isNull(publisher) ? null : new ScientificValue(publisher));
  }

  public record ScientificValue(URI id, String name, String scientificValue) {

    public ScientificValue(JsonNode source) {
      this(source.path(ID), source.path(NAME), source.path(SCIENTIFIC_VALUE));
    }

    public ScientificValue(JsonNode id, JsonNode name, JsonNode scientificValue) {
      this(
          NodeUtils.toUri(id),
          isNull(name) ? null : name.asText(),
          isNull(scientificValue) ? null : scientificValue.asText());
    }
  }
}
