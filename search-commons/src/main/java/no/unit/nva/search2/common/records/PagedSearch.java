package no.unit.nva.search2.common.records;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.JsonNode;
import java.net.URI;
import java.util.List;
import java.util.Map;
import no.unit.nva.commons.json.JsonSerializable;
import nva.commons.core.JacocoGenerated;

import static java.util.Objects.nonNull;
import static no.unit.nva.search2.common.constant.Defaults.PAGINATED_SEARCH_RESULT_CONTEXT;

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

    @JacocoGenerated    // this is tested, but is reported as not tested (remove nonNull Test and see the tests fail)
    @Override
    public Map<String, List<Facet>> aggregations() {
        return nonNull(aggregations)
            ? aggregations
            : Map.of();
    }
}
