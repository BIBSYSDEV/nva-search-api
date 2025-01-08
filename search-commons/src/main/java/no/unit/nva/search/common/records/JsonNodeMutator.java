package no.unit.nva.search.common.records;

import static no.unit.nva.search.resource.Constants.GLOBAL_EXCLUDED_FIELDS;

import com.fasterxml.jackson.databind.JsonNode;

import java.util.List;

public interface JsonNodeMutator {
    JsonNode transform(JsonNode source);

  static List<String> getExcludedFields() {
    return List.of(GLOBAL_EXCLUDED_FIELDS);
  }

  static List<String> getIncludedFields() {
    return List.of();
  }
}
