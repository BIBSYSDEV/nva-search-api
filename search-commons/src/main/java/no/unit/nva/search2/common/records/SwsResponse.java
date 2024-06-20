package no.unit.nva.search2.common.records;

import static java.util.Objects.nonNull;
import static java.util.Objects.requireNonNull;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.JsonNode;
import java.beans.Transient;
import java.util.List;
import java.util.Optional;
import no.unit.nva.search2.common.records.SwsResponse.HitsInfo.Hit;
import nva.commons.core.JacocoGenerated;

/**
 * Response from SWS, almost identical to Opensearch's response.
 * @author Stig Norland
 */
public record SwsResponse(
    int took,
    boolean timed_out,
    ShardsInfo _shards,
    HitsInfo hits,
    JsonNode aggregations,
    String _scroll_id) {

    public record ShardsInfo(
        Long total,
        Long successful,
        Long skipped,
        Long failed) {

    }

    public record HitsInfo(
        TotalInfo total,
        double max_score,
        List<Hit> hits) {
        public record TotalInfo(
            Integer value,
            String relation) {
        }

        @JsonInclude(JsonInclude.Include.NON_EMPTY)
        public record Hit(
            String _index,
            String _type,
            String _id,
            double _score,
            JsonNode _source,
            List<String> sort) {
        }
    }

    @Transient
    public Integer getTotalSize() {
        return hits.total.value;
    }

    @Transient
    public List<JsonNode> getSearchHits() {
        return hits.hits().stream().map(Hit::_source).toList();
    }

    @JacocoGenerated
    @Transient
    public List<String> getSort() {
        return
            nonNull(hits) && nonNull(hits.hits) && !hits.hits.isEmpty()
                ? Optional.ofNullable(hits.hits.get(hits.hits.size() - 1).sort())
                .orElse(List.of())
                : List.of();
    }

    public static final class SwsResponseBuilder {
        private String _scroll_id;
        private int took;
        private boolean timed_out;
        private ShardsInfo _shards;
        private HitsInfo hits;
        private JsonNode aggregations;

        private SwsResponseBuilder() {
        }

        public static SwsResponseBuilder swsResponseBuilder() {
            return new SwsResponseBuilder();
        }

        public SwsResponseBuilder withScrollId(String _scroll_id) {
            this._scroll_id = _scroll_id;
            return this;
        }

        public SwsResponseBuilder withTook(int took) {
            this.took = took;
            return this;
        }

        public SwsResponseBuilder withTimedOut(boolean timed_out) {
            this.timed_out = timed_out;
            return this;
        }

        public SwsResponseBuilder withShards(ShardsInfo _shards) {
            this._shards = _shards;
            return this;
        }

        public SwsResponseBuilder withHits(HitsInfo hits) {
            if (nonNull(hits) && hits.total().value() >= 0 ) {
                this.hits = hits;
            }
            return this;
        }

        public SwsResponseBuilder withAggregations(JsonNode aggregations) {
            if (!aggregations.isEmpty()) {
                this.aggregations = aggregations;
            }
            return this;
        }

        public SwsResponseBuilder merge(SwsResponse response){
            return withHits(response.hits())
                    .withAggregations(response.aggregations())
                    .withShards(response._shards())
                    .withScrollId(response._scroll_id())
                    .withTimedOut(response.timed_out())
                    .withTook(response.took());
        }

        public SwsResponseBuilder returnBuilder() {
            return this;
        }



        public SwsResponse build() {
            return new SwsResponse(took, timed_out, _shards, hits, aggregations, _scroll_id);
        }
    }

}
