package no.sikt.nva.oai.pmh.handler.oaipmh;

import static java.util.Objects.nonNull;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Optional;
import nva.commons.core.StringUtils;

public final class OaiPmhDateTime implements NullableWrapper<String> {
  public static final OaiPmhDateTime EMPTY_INSTANCE = new OaiPmhDateTime(null);
  private final Instant instant;

  private OaiPmhDateTime(Instant instant) {
    this.instant = instant;
  }

  public static OaiPmhDateTime from(String value) {
    if (StringUtils.isEmpty(value)) {
      return EMPTY_INSTANCE;
    }

    Instant instant;
    try {
      instant = Instant.parse(value);
    } catch (DateTimeParseException e) {
      instant = getInstantFromDate(value);
    }
    return new OaiPmhDateTime(instant);
  }

  @Override
  public boolean isPresent() {
    return nonNull(instant);
  }

  @Override
  public Optional<String> getValue() {
    return Optional.ofNullable(instant)
        .map(localInstant -> ZonedDateTime.ofInstant(localInstant, ZoneId.of("Z")))
        .map(DateTimeFormatter.ISO_DATE_TIME::format);
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
