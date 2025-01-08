package no.unit.nva.search.common.records;

import com.fasterxml.jackson.databind.JsonNode;

public interface JsonNodeMutator {
  JsonNode transform(JsonNode source);
}
