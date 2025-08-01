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
    ListRecordsRequest originalRequest, OaiPmhDateTime cursor, int totalSize) {
  private static final String EQUALS = "=";
  private static final String FROM = "from";
  private static final String UNTIL = "until";
  private static final String SET = "set";
  private static final String METADATA_PREFIX = "metadataPrefix";
  private static final String CURSOR = "cursor";
  private static final String TOTAL_SIZE = "totalSize";
  private static final String AMPERSAND = "&";

  public String getValue() {
    var unencodedValueBuilder = new StringBuilder();
    unencodedValueBuilder
        .append(METADATA_PREFIX)
        .append(EQUALS)
        .append(originalRequest.getMetadataPrefix().getPrefix());

    originalRequest
        .getFrom()
        .ifPresent(
            value ->
                unencodedValueBuilder.append(AMPERSAND).append(FROM).append(EQUALS).append(value));

    originalRequest
        .getUntil()
        .ifPresent(
            value ->
                unencodedValueBuilder.append(AMPERSAND).append(UNTIL).append(EQUALS).append(value));

    originalRequest
        .getSetSpec()
        .ifPresent(
            value ->
                unencodedValueBuilder.append(AMPERSAND).append(SET).append(EQUALS).append(value));

    cursor.ifPresent(
        value ->
            unencodedValueBuilder.append(AMPERSAND).append(CURSOR).append(EQUALS).append(value));
    unencodedValueBuilder.append(AMPERSAND).append(TOTAL_SIZE).append(EQUALS).append(totalSize);
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
            .map(part -> part.split(EQUALS))
            .collect(Collectors.toMap(part -> part[0], part -> part[1]));

    var metadataPrefix = values.get(METADATA_PREFIX);
    var from = values.get(FROM);
    var until = values.get(UNTIL);
    var set = values.get(SET);
    var cursor = values.get(CURSOR);
    var totalSize = values.get(TOTAL_SIZE);

    var originalRequest =
        new ListRecordsRequest(
            OaiPmhDateTime.from(from),
            OaiPmhDateTime.from(until),
            SetSpec.from(set),
            MetadataPrefix.fromPrefix(metadataPrefix));
    return Optional.of(
        new ResumptionToken(
            originalRequest, OaiPmhDateTime.from(cursor), Integer.parseInt(totalSize)));
  }
}
