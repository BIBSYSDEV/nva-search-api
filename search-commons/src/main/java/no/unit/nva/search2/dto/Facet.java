package no.unit.nva.search2.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.net.URI;
import java.util.Map;
import java.util.Objects;
import no.unit.nva.commons.json.JsonSerializable;

@JsonIgnoreProperties(ignoreUnknown = true)
public record Facet(
    URI id,
    String key,
    Integer count,
    Map<String, String> labels
) implements JsonSerializable {

    public Facet {
        Objects.requireNonNull(count);
    }
}
