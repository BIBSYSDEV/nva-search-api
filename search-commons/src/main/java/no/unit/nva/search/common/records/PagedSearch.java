package no.unit.nva.search.common.records;

import static java.util.Objects.nonNull;
import static no.unit.nva.search.common.constant.Defaults.PAGINATED_SEARCH_RESULT_CONTEXT;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.JsonNode;
import java.net.URI;
import java.util.List;
import java.util.Map;
import no.unit.nva.commons.json.JsonSerializable;

public record PagedSearch(
    URI id,
    Integer totalHits,
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

    @Override
    public Map<String, List<Facet>> aggregations() {
        return nonNull(aggregations)
            ? aggregations
            : Map.of();
    }
}
