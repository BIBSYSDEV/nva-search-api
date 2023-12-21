package no.unit.nva.search2.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.JsonNode;
import no.unit.nva.commons.json.JsonSerializable;

import java.net.URI;
import java.util.List;
import java.util.Map;

import static no.unit.nva.search2.constant.Defaults.PAGINATED_SEARCH_RESULT_CONTEXT;

public record PagedSearch(
    URI id,
    int totalHits,
    @JsonInclude()
    List<JsonNode> hits,
    URI nextResults,
    URI nextSearchAfterResults,
    URI previousResults,
    Map<String, List<Facet>> aggregations) implements JsonSerializable {

    @JsonProperty("@context")
    public URI context() {
        return PAGINATED_SEARCH_RESULT_CONTEXT;
    }

}
