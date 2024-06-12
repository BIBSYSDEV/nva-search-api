package no.unit.nva.search2.common.records;

import static java.util.Objects.nonNull;
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
}
