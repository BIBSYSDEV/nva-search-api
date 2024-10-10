package no.unit.nva.search.model.records;

import static no.unit.nva.search.model.constant.Defaults.PAGINATED_SEARCH_RESULT_CONTEXT;

import static java.util.Objects.nonNull;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.JsonNode;

import no.unit.nva.commons.json.JsonSerializable;

import java.net.URI;
import java.util.List;
import java.util.Map;

/**
 * PagedSearch is a class that represents a paged search result.
 *
 * @author Stig Norland
 * @param id the URI of the search result. The URI is a unique identifier for the search result.
 * @param totalHits the total number of hits in the search result.
 * @param hits the hits in the search result.
 * @param nextResults the URI of the next results.
 * @param nextSearchAfterResults the URI of the next search after results.
 * @param previousResults the URI of the previous results.
 * @param aggregations the aggregations in the search result.
 */
public record PagedSearch(
        URI id,
        Integer totalHits,
        @JsonInclude() List<JsonNode> hits,
        URI nextResults,
        URI nextSearchAfterResults,
        URI previousResults,
        Map<String, List<Facet>> aggregations)
        implements JsonSerializable {

    @JsonProperty("@context")
    public URI context() {
        return PAGINATED_SEARCH_RESULT_CONTEXT;
    }

    @Override
    public Map<String, List<Facet>> aggregations() {
        return nonNull(aggregations) ? aggregations : Map.of();
    }
}
