package no.unit.nva.search.common.records;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.net.URI;
import java.util.Map;
import java.util.Objects;
import no.unit.nva.commons.json.JsonSerializable;

/**
 * Facet is a class that represents a facet in a search result.
 *
 * @author Stig Norland
 * @param id the URI of the facet. The URI is a unique identifier for the facet.
 * @param key the key of the facet.
 * @param count the count of the facet.
 * @param labels the labels of the facet.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record Facet(
        URI id, String key, @JsonAlias("doc_count") Integer count, Map<String, String> labels)
        implements JsonSerializable {

    public Facet {
        Objects.requireNonNull(count);
    }
}
