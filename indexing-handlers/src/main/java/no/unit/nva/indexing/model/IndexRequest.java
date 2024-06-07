package no.unit.nva.indexing.model;

import com.fasterxml.jackson.core.type.TypeReference;
import no.unit.nva.commons.json.JsonUtils;

import java.util.Collections;
import java.util.Map;

import static nva.commons.core.attempt.Try.attempt;

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
        var typeReference = new TypeReference<Map<String, Object>>() {
        };
        this.mappings = attempt(() -> JsonUtils.dtoObjectMapper.readValue(jsonMappings, typeReference))
                .orElseThrow();
        this.settings = Collections.emptyMap();
    }

    public IndexRequest(String name, String jsonMappings, String jsonSettings) {
        this.name = name;
        var typeReference = new TypeReference<Map<String, Object>>() {
        };
        this.mappings = attempt(() -> JsonUtils.dtoObjectMapper.readValue(jsonMappings, typeReference))
            .orElseThrow();
        this.settings = attempt(() -> JsonUtils.dtoObjectMapper.readValue(jsonSettings, typeReference))
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

