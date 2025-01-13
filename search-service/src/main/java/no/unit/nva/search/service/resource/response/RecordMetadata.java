package no.unit.nva.search.service.resource.response;

import com.fasterxml.jackson.databind.JsonNode;
import org.joda.time.DateTime;

public record RecordMetadata(
    String status, DateTime createdDate, DateTime modifiedDate, DateTime publishedDate) {

  public RecordMetadata(
      JsonNode status, JsonNode createdDate, JsonNode modifiedDate, JsonNode publishedDate) {
    this(
        status.asText(),
        new DateTime(createdDate.asText()),
        new DateTime(modifiedDate.asText()),
        new DateTime(publishedDate.asText()));
  }
}
