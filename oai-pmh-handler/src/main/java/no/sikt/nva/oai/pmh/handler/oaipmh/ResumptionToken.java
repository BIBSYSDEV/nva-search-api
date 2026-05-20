package no.sikt.nva.oai.pmh.handler.oaipmh;

import java.net.URLDecoder;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Optional;
import java.util.stream.Collectors;
import no.sikt.nva.oai.pmh.handler.oaipmh.request.ListRecordsRequest;
import nva.commons.core.StringUtils;

public record ResumptionToken(
    ListRecordsRequest originalRequest, String searchAfter, int totalSize) {
  private static final String EQUALS = "=";
  private static final String FROM = "from";
  private static final String UNTIL = "until";
  private static final String SET = "set";
  private static final String METADATA_PREFIX = "metadataPrefix";
  private static final String SEARCH_AFTER = "searchAfter";
  private static final String LEGACY_CURSOR = "cursor";
  private static final String TOTAL_SIZE = "totalSize";
  private static final String AMPERSAND = "&";
  private static final String COMMA = ",";

  public String getValue() {
    var unencodedValueBuilder = new StringBuilder(128);
    unencodedValueBuilder
        .append(METADATA_PREFIX)
        .append(EQUALS)
        .append(originalRequest.getMetadataPrefix().getPrefix());

    originalRequest
        .getFrom()
        .ifPresent(
            ignored ->
                unencodedValueBuilder
                    .append(AMPERSAND)
                    .append(FROM)
                    .append(EQUALS)
                    .append(originalRequest.getFrom().getOriginalSource().orElseThrow()));

    originalRequest
        .getUntil()
        .ifPresent(
            ignored ->
                unencodedValueBuilder
                    .append(AMPERSAND)
                    .append(UNTIL)
                    .append(EQUALS)
                    .append(originalRequest.getUntil().getOriginalSource().orElseThrow()));

    originalRequest
        .getSetSpec()
        .ifPresent(
            value ->
                unencodedValueBuilder.append(AMPERSAND).append(SET).append(EQUALS).append(value));

    unencodedValueBuilder
        .append(AMPERSAND)
        .append(SEARCH_AFTER)
        .append(EQUALS)
        .append(searchAfter)
        .append(AMPERSAND)
        .append(TOTAL_SIZE)
        .append(EQUALS)
        .append(totalSize);
    return URLEncoder.encode(unencodedValueBuilder.toString(), StandardCharsets.UTF_8);
  }

  public static Optional<ResumptionToken> from(String encodedToken) {
    if (StringUtils.isEmpty(encodedToken)) {
      return Optional.empty();
    }

    var unencodedToken = URLDecoder.decode(encodedToken, StandardCharsets.UTF_8);
    String[] parts = unencodedToken.split(AMPERSAND);

    var values =
        Arrays.stream(parts)
            .map(part -> part.split(EQUALS, 2))
            .collect(Collectors.toMap(part -> part[0], part -> part[1]));

    var metadataPrefix = values.get(METADATA_PREFIX);
    var from = values.get(FROM);
    var until = values.get(UNTIL);
    var set = values.get(SET);
    var searchAfter = values.getOrDefault(SEARCH_AFTER, migrateLegacyCursor(values.get(LEGACY_CURSOR)));
    var totalSize = values.get(TOTAL_SIZE);

    var originalRequest =
        new ListRecordsRequest(
            OaiPmhDateTime.from(from),
            OaiPmhDateTime.from(until),
            SetSpec.from(set),
            MetadataPrefix.fromPrefix(metadataPrefix));
    return Optional.of(
        new ResumptionToken(originalRequest, searchAfter, Integer.parseInt(totalSize)));
  }

  // Backward-compatibility: tokens issued before NP-51203 use cursor=<modifiedDate>
  // (lastModifiedDate + 1 ns) instead of searchAfter=<modifiedDate>,<identifier>.
  // Map them into the new tuple shape with an empty identifier so a harvest in
  // flight at deploy time continues without restarting. The first page after the
  // upgrade may include up to one batch of duplicates at the boundary; that mirrors
  // the legacy behaviour and is far cheaper than forcing a fresh harvest.
  private static String migrateLegacyCursor(String legacyCursor) {
    return legacyCursor == null ? null : legacyCursor + COMMA;
  }
}
