package no.sikt.nva.oai.pmh.handler.oaipmh;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

public enum MetadataPrefix {
  OAI_DC("oai_dc");

  private static final Map<String, MetadataPrefix> prefixMap = new HashMap<>();

  static {
    for (MetadataPrefix metadataPrefix : values()) {
      prefixMap.put(metadataPrefix.getPrefix(), metadataPrefix);
    }
  }

  private final String prefix;

  MetadataPrefix(String prefix) {
    this.prefix = prefix;
  }

  public String getPrefix() {
    return prefix;
  }

  public static MetadataPrefix fromPrefix(String prefix) {
    return Optional.ofNullable(prefixMap.get(prefix))
        .orElseThrow(
            () -> new MetadataPrefixNotSupportedException("metadataPrefix is not supported."));
  }
}
