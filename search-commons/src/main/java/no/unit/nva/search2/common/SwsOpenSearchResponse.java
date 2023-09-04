package no.unit.nva.search2.common;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.JsonNode;
import no.unit.nva.search2.common.SwsOpenSearchResponse.HitsInfo.Hit;

import java.net.URI;
import java.util.List;

import static no.unit.nva.search.models.SearchResponseDto.formatAggregations;
import static no.unit.nva.search2.constants.Defaults.DEFAULT_SEARCH_CONTEXT;

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
        var sourcesList =
            hits().hits().stream()
                .map(Hit::_source).toList();
        return new PagedSearchResponseDto(
            DEFAULT_SEARCH_CONTEXT,
            requestUri,
            PagedSearchResponseDto.nextResults(requestUri),
            PagedSearchResponseDto.previousResults(requestUri),
            took(),
            hits().total().value(),
            sourcesList,
            extractAggregations(aggregations));
    }

    private static JsonNode extractAggregations(JsonNode aggregations) {
        if (aggregations == null) {
            return null;
        }
        return formatAggregations(aggregations);
    }
}
