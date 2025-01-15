package no.unit.nva.search.resource.response;

import static java.util.Objects.isNull;
import static no.unit.nva.constants.Words.DOI;
import static no.unit.nva.constants.Words.ID;
import static no.unit.nva.constants.Words.NAME;
import static no.unit.nva.constants.Words.PUBLICATION_CONTEXT;
import static no.unit.nva.constants.Words.PUBLISHER;
import static no.unit.nva.constants.Words.SCIENTIFIC_VALUE;
import static no.unit.nva.constants.Words.SERIES;
import static no.unit.nva.constants.Words.TYPE;

import com.fasterxml.jackson.databind.JsonNode;
import java.net.URI;

public record PublishingDetails(
    URI id, String type, String name, URI doi, ScientificValue series, ScientificValue publisher) {

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

  public PublishingDetails(JsonNode reference) {
    this(
        reference.path(PUBLICATION_CONTEXT).path(ID),
        reference.path(PUBLICATION_CONTEXT).path(TYPE),
        reference.path(PUBLICATION_CONTEXT).path(NAME),
        reference.path(DOI),
        reference.path(PUBLICATION_CONTEXT).path(SERIES),
        reference.path(PUBLICATION_CONTEXT).path(PUBLISHER));
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