package no.unit.nva.search.resource.response;

import com.fasterxml.jackson.databind.JsonNode;

public record PublicationDate(String year, String month, String day) {
  public PublicationDate(JsonNode publicationDate) {
    this(
        publicationDate.get("year").asText(),
        publicationDate.get("month").asText(),
        publicationDate.get("day").asText());
  }
}
