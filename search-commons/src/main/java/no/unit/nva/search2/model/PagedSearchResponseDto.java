package no.unit.nva.search2.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.JsonNode;

import java.net.URI;
import java.util.List;

import static no.unit.nva.search2.constant.Defaults.PAGINATED_SEARCH_RESULT_CONTEXT;

public record PagedSearchResponseDto (
    @JsonProperty("@context")URI context,
    URI previousResults,
    URI id,
    URI nextResults,
    long processingTime,
    long size,
    List<JsonNode> hits,
    JsonNode aggregations)  {

    @Override
    public URI context() {
        return URI.create(PAGINATED_SEARCH_RESULT_CONTEXT);
    }

}
