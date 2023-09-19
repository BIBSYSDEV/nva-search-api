package no.unit.nva.search2.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.JsonNode;
import no.unit.nva.search2.model.OpenSearchSwsResponse.HitsInfo.Hit;

import java.beans.Transient;
import java.util.List;
import java.util.Optional;

import org.jetbrains.annotations.NotNull;

import static java.util.Objects.nonNull;
import static no.unit.nva.search.models.SearchResponseDto.formatAggregations;

public record OpenSearchSwsResponse(
    int took,
    boolean timed_out,
    ShardsInfo _shards,
    HitsInfo hits,
    JsonNode aggregations) {

    public record ShardsInfo(
        int total,
        int successful,
        int skipped,
        int failed) {

    }

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
            List<Long> sort) {

        }
    }

    @NotNull
    @Transient
    public Integer getTotalSize() {
        return nonNull(hits)
                   ? hits.total.value
                   : 0;
    }

    @NotNull
    @Transient
    public List<JsonNode> getSearchHits() {
        return
            nonNull(hits) && nonNull(hits.hits)
                ? hits.hits().stream().map(Hit::_source).toList()
                : List.of();
    }

    @NotNull
    @Transient
    public List<Long> getSort() {
        return
            nonNull(hits) && nonNull(hits.hits) && !hits.hits.isEmpty()
                ? Optional.ofNullable(hits.hits.get(hits.hits.size() - 1).sort())
                    .orElse(List.of())
                : List.of();
    }


    @Transient
    public JsonNode getAggregationsStructured() {
        return nonNull(aggregations)
                   ? formatAggregations(aggregations)
                   : null;
    }
}
