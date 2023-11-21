package no.unit.nva.search2.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

import java.net.URI;
import java.util.Map;
import java.util.Objects;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonTypeInfo(use = JsonTypeInfo.Id.DEDUCTION)
public record Facet(URI id, Integer count, String key, Map<String, String> labels) {

    public Facet {
        Objects.requireNonNull(count);
        Objects.requireNonNull(key);
        Objects.requireNonNull(labels);
    }
}
