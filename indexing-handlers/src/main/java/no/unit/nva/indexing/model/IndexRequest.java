package no.unit.nva.indexing.model;

import static nva.commons.core.attempt.Try.attempt;

import com.fasterxml.jackson.core.type.TypeReference;
import java.util.Collections;
import java.util.Map;
import no.unit.nva.commons.json.JsonUtils;

public class IndexRequest {
  private final String name;
  private final Map<String, Object> mappings;
  private final Map<String, Object> settings;

  public IndexRequest(String name) {
    this.name = name;
    this.mappings = Collections.emptyMap();
    this.settings = Collections.emptyMap();
  }

  public IndexRequest(String name, String jsonMappings) {
    this.name = name;
    this.mappings = jsonToJavaMap(jsonMappings);
    this.settings = Collections.emptyMap();
  }

  public IndexRequest(String name, String jsonMappings, String jsonSettings) {
    this.name = name;
    this.mappings = jsonToJavaMap(jsonMappings);
    this.settings = jsonToJavaMap(jsonSettings);
  }

  private static Map<String, Object> jsonToJavaMap(String jsonMappings) {
    var typeReference = new TypeReference<Map<String, Object>>() {};
    return attempt(() -> JsonUtils.dtoObjectMapper.readValue(jsonMappings, typeReference))
        .orElseThrow();
  }

  public String getName() {
    return name;
  }

  public Map<String, Object> getMappings() {
    return mappings;
  }

  public Map<String, Object> getSettings() {
    return settings;
  }
}
