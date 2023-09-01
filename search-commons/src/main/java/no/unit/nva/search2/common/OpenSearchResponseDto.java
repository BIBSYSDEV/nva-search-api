package no.unit.nva.search2.common;

import static no.unit.nva.search.models.SearchResponseDto.formatAggregations;
import static no.unit.nva.search2.constants.Defaults.DEFAULT_SEARCH_CONTEXT;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.databind.JsonNode;
import java.net.URI;
import java.util.List;
import no.unit.nva.search.models.SearchResponseDto;
import no.unit.nva.search2.common.OpenSearchResponseDto.HitsInfo.Hit;

public record OpenSearchResponseDto(int took, boolean timed_out, ShardsInfo _shards, HitsInfo hits, JsonNode aggregations) {

    public record ShardsInfo(int total, int successful, int skipped, int failed) {}

    public record HitsInfo(TotalInfo total, double max_score, List<Hit> hits) {

        public record TotalInfo(int value, String relation) {}

        public record Hit(String _index, String _type, String _id, double _score, JsonNode _source) {}
    }

    @JsonIgnore
    public  SearchResponseDto toSearchResponseDto(URI requestUri) {
        var sourcesList =
            hits().hits().stream()
                .map(Hit::_source).toList();

        return SearchResponseDto.builder()
                   .withContext(DEFAULT_SEARCH_CONTEXT)
                   .withId(requestUri)
                   .withHits(sourcesList)
                   .withSize(hits().total().value())
                   .withProcessingTime(took())
                   .withAggregations(extractAggregations(aggregations))
                   .build();
    }

    private static JsonNode extractAggregations(JsonNode aggregations) {
        if (aggregations == null) {
            return null;
        }
        return formatAggregations(aggregations);
    }
}
