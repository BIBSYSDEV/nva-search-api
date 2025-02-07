package no.sikt.nva.oai.pmh.handler;

import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import nva.commons.core.StringUtils;

public final class FormUrlencodedBodyParser {

  private static final String AMPERSAND = "&";
  private static final String EQUALS_SIGN = "=";
  private static final int SPLIT_PARTS_LIMIT = 2;
  private static final int ONE = 1;

  private Map<String, String> parameters;

  private FormUrlencodedBodyParser(String body) {
    if (StringUtils.isEmpty(body)) {
      throw new IllegalArgumentException("Body cannot be null or empty");
    }
    parse(body);
  }

  public static FormUrlencodedBodyParser from(String source) {
    return new FormUrlencodedBodyParser(source);
  }

  private void parse(String body) {
    var pairs = body.split(AMPERSAND);
    if (pairs.length < ONE) {
      throw new IllegalArgumentException("Invalid form-urlencoded format");
    }

    parameters =
        Arrays.stream(pairs)
            .map(this::splitPairs)
            .collect(Collectors.toMap(this::extractKey, this::extractValue));
  }

  private String extractValue(String... keyValue) {
    return URLDecoder.decode(keyValue[1], StandardCharsets.UTF_8);
  }

  private String extractKey(String... keyValue) {
    var key = URLDecoder.decode(keyValue[0], StandardCharsets.UTF_8);
    if (StringUtils.isEmpty(key)) {
      throw new IllegalArgumentException("Key cannot be null or empty");
    }
    return key;
  }

  private String[] splitPairs(String pair) {
    var keyValue = pair.split(EQUALS_SIGN, SPLIT_PARTS_LIMIT);
    if (keyValue.length != SPLIT_PARTS_LIMIT) {
      throw new IllegalArgumentException("Invalid form-urlencoded format");
    }
    return keyValue;
  }

  public Optional<String> getValue(final String key) {
    return Optional.ofNullable(parameters.get(key));
  }
}
