package no.sikt.nva.oai.pmh.handler;

import static java.util.Objects.nonNull;

import java.net.URLDecoder;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Optional;
import java.util.stream.Collectors;
import nva.commons.core.StringUtils;

public record ResumptionToken(
    String from, String until, String metadataPrefix, String current, int totalSize) {
  private static final String EQUALS = "=";
  private static final String FROM = "from";
  private static final String UNTIL = "until";
  private static final String METADATA_PREFIX = "metadataPrefix";
  private static final String CURRENT = "current";
  private static final String TOTAL_SIZE = "totalSize";
  private static final String AMPERSAND = "&";

  public String getValue() {
    var unencodedValueBuilder = new StringBuilder();
    unencodedValueBuilder.append(METADATA_PREFIX).append(EQUALS).append(metadataPrefix);
    if (nonNull(from)) {
      unencodedValueBuilder.append(AMPERSAND).append(FROM).append(EQUALS).append(from);
    }
    if (nonNull(until)) {
      unencodedValueBuilder.append(AMPERSAND).append(UNTIL).append(EQUALS).append(until);
    }
    if (nonNull(current)) {
      unencodedValueBuilder.append(AMPERSAND).append(CURRENT).append(EQUALS).append(current);
    }
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
    var current = values.get(CURRENT);
    var totalSize = values.get(TOTAL_SIZE);

    return Optional.of(
        new ResumptionToken(from, until, metadataPrefix, current, Integer.parseInt(totalSize)));
  }
}
