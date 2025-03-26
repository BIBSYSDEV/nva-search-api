package no.unit.nva.constants;

import static nva.commons.core.attempt.Try.attempt;
import static nva.commons.core.ioutils.IoUtils.stringFromResources;

import com.fasterxml.jackson.core.type.TypeReference;
import java.nio.file.Path;
import java.util.Collections;
import java.util.Map;
import no.unit.nva.commons.json.JsonUtils;

public final class IndexMappingsAndSettings {
  public static final IndexConfiguration RESOURCE_MAPPINGS;
  public static final IndexConfiguration TICKET_MAPPINGS;
  public static final IndexConfiguration IMPORT_CANDIDATE_MAPPINGS;
  public static final IndexConfiguration RESOURCE_SETTINGS;

  static {
    RESOURCE_MAPPINGS = loadMapFromResource("indices/resource/mappings.json");
    TICKET_MAPPINGS = loadMapFromResource("indices/ticket/mappings.json");
    IMPORT_CANDIDATE_MAPPINGS = loadMapFromResource("indices/import_candidate/mappings.json");
    RESOURCE_SETTINGS = loadMapFromResource("indices/resource/settings.json");
  }

  public static final class IndexConfiguration {
    private final String json;
    private final Map<String, Object> map;

    private IndexConfiguration(String json, Map<String, Object> map) {
      this.json = json;
      this.map = Collections.unmodifiableMap(map);
    }

    public String asJson() {
      return json;
    }

    public Map<String, Object> asMap() {
      return map;
    }
  }

  private static IndexConfiguration loadMapFromResource(String resourcePath) {
    var json = stringFromResources(Path.of(resourcePath));
    var type = new TypeReference<Map<String, Object>>() {};
    var map = attempt(() -> JsonUtils.dtoObjectMapper.readValue(json, type)).orElseThrow();

    return new IndexConfiguration(json, map);
  }

  private IndexMappingsAndSettings() {}
}
