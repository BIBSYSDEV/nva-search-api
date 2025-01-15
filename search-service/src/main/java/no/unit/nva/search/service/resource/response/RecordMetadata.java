package no.unit.nva.search.service.resource.response;

import static java.util.Objects.isNull;

import com.fasterxml.jackson.databind.JsonNode;
import org.joda.time.DateTime;

public record RecordMetadata(
    String status, DateTime createdDate, DateTime modifiedDate, DateTime publishedDate) {

  public RecordMetadata(
      JsonNode status, JsonNode createdDate, JsonNode modifiedDate, JsonNode publishedDate) {
    this(
        isNull(status) ? null : status.asText(),
        isNull(createdDate) ? null : new DateTime(createdDate.textValue()),
        isNull(modifiedDate) ? null : new DateTime(modifiedDate.textValue()),
        isNull(publishedDate) ? null : new DateTime(publishedDate.textValue()));
  }
}
