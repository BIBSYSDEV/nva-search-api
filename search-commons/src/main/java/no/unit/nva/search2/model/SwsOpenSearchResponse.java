package no.unit.nva.search2.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.JsonNode;
import java.util.Optional;
import no.unit.nva.search2.model.SwsOpenSearchResponse.HitsInfo.Hit;

import java.net.URI;
import java.util.List;
import no.unit.nva.search2.model.SwsOpenSearchResponse.HitsInfo.TotalInfo;
import org.jetbrains.annotations.NotNull;

import static no.unit.nva.search.models.SearchResponseDto.formatAggregations;
import static no.unit.nva.search2.constant.Defaults.DEFAULT_SEARCH_CONTEXT;
import static no.unit.nva.search2.model.PagedSearchResponseDto.nextResults;
import static no.unit.nva.search2.model.PagedSearchResponseDto.previousResults;

public record SwsOpenSearchResponse(
    int took,
    boolean timed_out,
    ShardsInfo _shards,
    HitsInfo hits,
    JsonNode aggregations) {

    public record ShardsInfo(
        int total,
        int successful,
        int skipped,
        int failed) {}

    public record HitsInfo(
        TotalInfo total,
        double max_score,
        List<Hit> hits) {
        public record TotalInfo(
            int value,
            String relation) {
        }

        @JsonInclude(JsonInclude.Include.NON_EMPTY)
        public record Hit(
            String _index,
            String _type,
            String _id,
            double _score,
            JsonNode _source,
            Long[] sort) {
        }
    }

    @JsonIgnore
    public PagedSearchResponseDto toPagedSearchResponseDto(URI requestUri) {

        return new PagedSearchResponseDto(
            DEFAULT_SEARCH_CONTEXT,
            requestUri,
            nextResults(requestUri),
            previousResults(requestUri),
            took(),
            getSize(),
            getHits(),
            getAggregations());
    }

    @NotNull
    private Integer getSize() {
        return Optional.ofNullable(hits().total())
                   .orElse(new TotalInfo(0,""))
                   .value();
    }

    @NotNull
    private List<JsonNode> getHits() {
        return Optional.ofNullable(hits().hits())
                   .orElse(List.of())
                   .stream()
                   .map(Hit::_source)
                   .toList();
    }

    private JsonNode getAggregations() {
        if (aggregations == null) {
            return null;
        }
        return formatAggregations(aggregations);
    }
}
