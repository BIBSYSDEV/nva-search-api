package no.sikt.nva.oai.pmh.handler.oaipmh;

import java.time.Instant;

public final class OaiPmhDateTimeUtils {

  private OaiPmhDateTimeUtils() {}

  public static String truncateToSeconds(String iso8601Instant) {
    return Instant.parse(iso8601Instant)
        .truncatedTo(java.time.temporal.ChronoUnit.SECONDS)
        .toString();
  }
}
