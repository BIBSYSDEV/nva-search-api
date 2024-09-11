package no.unit.nva.search.common.records;

import com.fasterxml.jackson.databind.JsonNode;

/**
 * Midas was a mythological king who transmuted everything he touched into gold.
 */
public interface Midas {
    JsonNode transform(JsonNode source);
}
