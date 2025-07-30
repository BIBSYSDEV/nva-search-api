package no.sikt.nva.oai.pmh.handler.oaipmh;

import java.util.Arrays;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import nva.commons.core.StringUtils;

public record SetSpec(SetRoot root, String... children) {

  private static final String COLON = ":";
  private static final int ZERO = 0;
  private static final int ONE = 1;

  public static SetSpec parse(final String value) {
    if (StringUtils.isEmpty(value)) {
      throw new BadArgumentException("Illegal set spec. Value is empty.");
    }

    var parts = value.split(COLON);
    if (parts.length == ONE) {
      return new SetSpec(SetRoot.fromValue(parts[ZERO]));
    } else {
      return new SetSpec(
          SetRoot.fromValue(parts[ZERO]), Arrays.copyOfRange(parts, ONE, parts.length));
    }
  }

  public String asString() {
    var valueBuilder = new StringBuilder(root.value);
    Arrays.stream(children).forEach(child -> valueBuilder.append(COLON).append(child));
    return valueBuilder.toString();
  }

  public enum SetRoot {
    RESOURCE_TYPE_GENERAL("resourceTypeGeneral");

    private static final Map<String, SetRoot> byValueMap = new ConcurrentHashMap<>();

    static {
      for (SetRoot setRoot : values()) {
        byValueMap.put(setRoot.getValue(), setRoot);
      }
    }

    private final String value;

    SetRoot(String value) {
      this.value = value;
    }

    public String getValue() {
      return value;
    }

    public static SetRoot fromValue(final String value) {
      return Optional.ofNullable(byValueMap.get(value))
          .orElseThrow(() -> new BadArgumentException("Illegal set spec. Unknown root."));
    }
  }
}
