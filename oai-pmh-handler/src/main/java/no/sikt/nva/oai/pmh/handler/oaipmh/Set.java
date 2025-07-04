package no.sikt.nva.oai.pmh.handler.oaipmh;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

public enum Set {
  PUBLICATION_INSTANCE_TYPE("PublicationInstanceType");

  private static final Map<String, Set> valueMap = new HashMap<>();

  static {
    for (Set set : values()) {
      valueMap.put(set.value, set);
    }
  }

  private static final String COLON = ":";

  private final String value;

  Set(String value) {
    this.value = value;
  }

  public static Set from(String value) {
    return Optional.ofNullable(valueMap.get(value))
        .orElseThrow(() -> new SetNotSupportedException(value));
  }

  public String getValue() {
    return value;
  }

  public String getSpec(String type) {
    return getValue() + COLON + type;
  }
}
