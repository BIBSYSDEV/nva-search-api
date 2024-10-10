package no.unit.nva.search.model.records;

import com.fasterxml.jackson.databind.JsonNode;

public interface JsonNodeMutator {
    JsonNode transform(JsonNode source);
}
