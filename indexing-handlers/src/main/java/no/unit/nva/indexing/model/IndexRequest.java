package no.unit.nva.indexing.model;

import com.fasterxml.jackson.core.type.TypeReference;
import no.unit.nva.commons.json.JsonUtils;

import java.util.Collections;
import java.util.Map;

import static nva.commons.core.attempt.Try.attempt;

public record IndexRequest(
    String name,
    Map<String, Object> mappings,
    Map<String, Object> settings
) {

    public IndexRequest(String name) {
        this(name, Collections.emptyMap(), Collections.emptyMap());
    }

    public IndexRequest(String name, String jsonMappings) {
        this(name, jsonToJavaMap(jsonMappings), Collections.emptyMap());
    }

    public IndexRequest(String name, String jsonMappings, String jsonSettings) {
        this(name, jsonToJavaMap(jsonMappings), jsonToJavaMap(jsonSettings));
    }

    private static Map<String, Object> jsonToJavaMap(String jsonMappings) {
        var typeReference = new TypeReference<Map<String, Object>>() {
        };
        return attempt(() -> JsonUtils.dtoObjectMapper.readValue(jsonMappings, typeReference))
            .orElseThrow();
    }
}

