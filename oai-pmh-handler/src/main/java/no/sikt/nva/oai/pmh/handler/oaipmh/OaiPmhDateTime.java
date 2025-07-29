package no.sikt.nva.oai.pmh.handler.oaipmh;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;

public final class OaiPmhDateTime {
  private final Instant instant;

  private OaiPmhDateTime(Instant instant) {
    this.instant = instant;
  }

  public static OaiPmhDateTime from(String value) {
    Instant instant;
    try {
      instant = Instant.parse(value);
    } catch (DateTimeParseException e) {
      instant = getInstantFromDate(value);
    }
    return new OaiPmhDateTime(instant);
  }

  public String asString() {
    var zonedDateTime = ZonedDateTime.ofInstant(instant, ZoneId.of("Z"));
    return DateTimeFormatter.ISO_DATE_TIME.format(zonedDateTime);
  }

  private static Instant getInstantFromDate(String value) {
    try {
      return LocalDate.parse(value, DateTimeFormatter.ISO_DATE)
          .atStartOfDay(ZoneId.of("UTC"))
          .toInstant();
    } catch (DateTimeParseException e) {
      throw new BadArgumentException("Invalid date provided in 'from' or 'until' parameter.");
    }
  }
}
