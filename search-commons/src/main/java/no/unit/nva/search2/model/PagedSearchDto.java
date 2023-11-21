package no.unit.nva.search2.model;

import static java.util.Objects.nonNull;
import static no.unit.nva.search2.constant.Defaults.PAGINATED_SEARCH_RESULT_CONTEXT;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.JsonNode;
import java.net.URI;
import java.util.List;
import java.util.Map;
import no.unit.nva.commons.json.JsonSerializable;

public record PagedSearchDto(
    URI id,
    int totalHits,
    List<JsonNode> hits,
    URI nextResults,
    URI nextSearchAfterResults,
    URI previousResults,
    Map<String, List<Facet>> aggregations) implements JsonSerializable {

    @JsonProperty("@context")
    public URI context() {
        return PAGINATED_SEARCH_RESULT_CONTEXT;
    }

    @Override
    public List<JsonNode> hits() {
        return nonNull(hits) ? hits : List.of();
    }
}
