package no.sikt.nva.oai.pmh.handler;

import static java.util.Objects.nonNull;

import java.net.URLDecoder;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.stream.Collectors;

public record ResumptionToken(String from, String until, String metadataPrefix, String scrollId) {
  private static final String EQUALS = "=";
  private static final String FROM = "from";
  private static final String UNTIL = "until";
  private static final String SCROLL_ID = "scrollId";
  private static final String METADATA_PREFIX = "metadataPrefix";
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
    if (nonNull(scrollId)) {
      unencodedValueBuilder.append(AMPERSAND).append(SCROLL_ID).append(EQUALS).append(scrollId);
    }
    return URLEncoder.encode(unencodedValueBuilder.toString(), StandardCharsets.UTF_8);
  }

  public static ResumptionToken from(String encodedToken) {
    var unencodedToken = URLDecoder.decode(encodedToken, StandardCharsets.UTF_8);
    String[] parts = unencodedToken.split(AMPERSAND);

    var values =
        Arrays.stream(parts)
            .map(part -> part.split(EQUALS))
            .collect(Collectors.toMap(part -> part[0], part -> part[1]));

    var metadataPrefix = values.get(METADATA_PREFIX);
    var from = values.get(FROM);
    var until = values.get(UNTIL);
    var scrollId = values.get(SCROLL_ID);

    return new ResumptionToken(from, until, metadataPrefix, scrollId);
  }
}
