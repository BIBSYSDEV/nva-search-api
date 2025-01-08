package no.unit.nva.search.service.resource.response;

import org.joda.time.DateTime;

import java.time.Instant;

public record PublicationDate(String year, String month, String day) {

  public PublicationDate fromInstant(Instant isoDate) {
    return fromDateTime(new DateTime(isoDate.toEpochMilli()));
  }

  public PublicationDate fromDateTime(DateTime dateTime) {
    return new PublicationDate(
        dateTime.year().getAsText(),
        dateTime.monthOfYear().getAsText(),
        dateTime.dayOfMonth().getAsText());
  }
}
