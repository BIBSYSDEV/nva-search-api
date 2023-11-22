package no.unit.nva.search2.model.opensearch;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.JsonNode;
import no.unit.nva.search2.model.opensearch.SwsResponse.HitsInfo.Hit;

import java.beans.Transient;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import nva.commons.core.JacocoGenerated;
import org.jetbrains.annotations.NotNull;

import static java.util.Objects.nonNull;
import static no.unit.nva.search.constants.ApplicationConstants.LABELS;
import static no.unit.nva.search.constants.ApplicationConstants.NAME;
import static no.unit.nva.search.constants.ApplicationConstants.objectMapperWithEmpty;
import static no.unit.nva.search2.constant.Patterns.PATTERN_IS_IGNORE_CASE;

public record SwsResponse(
    int took,
    boolean timed_out,
    ShardsInfo _shards,
    HitsInfo hits,
    JsonNode aggregations) {

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

    @JacocoGenerated
    @NotNull
    @Transient
    public Integer getTotalSize() {
        return nonNull(hits)
            ? hits.total.value
            : 0;
    }

    @JacocoGenerated
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
    public List<String> getSort() {
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

    public static final String WORD_ENDING_WITH_HASHTAG_REGEX = "[A-za-z0-9]*#";

    private static final Map<String, String> AGGREGATION_FIELDS_TO_CHANGE = Map.of(
        "docCount", "count",
        "doc_count", "count");

    public static JsonNode formatAggregations(JsonNode aggregations) {
        var outputAggregationNode = objectMapperWithEmpty.createObjectNode();

        var iterator = aggregations.fields();
        while (iterator.hasNext()) {
            var nodeEntry = iterator.next();
            var fieldName = nodeEntry.getKey();

            if (fieldName.matches(PATTERN_IS_IGNORE_CASE + "doc.?count.?error.?upper.?bound")) {
                continue;
            }
            if (fieldName.matches(PATTERN_IS_IGNORE_CASE + "sum.?other.?doc.?count")) {
                continue;
            }

            var newName = Optional.ofNullable(AGGREGATION_FIELDS_TO_CHANGE.get(fieldName))
                .orElse(fieldName.replaceFirst(WORD_ENDING_WITH_HASHTAG_REGEX, ""));

            var value = nodeEntry.getValue();
            if (LABELS.equals(newName)) {
                outputAggregationNode.set(newName, formatLabels(value));
            } else if (NAME.equals(newName)) {
                outputAggregationNode.set(LABELS, formatName(value));
            } else if (value.isValueNode()) {
                outputAggregationNode.set(newName, value);
            } else if (value.has( "buckets")) {
                var bucket = value.get("buckets");
                var arrayNode = objectMapperWithEmpty.createArrayNode();
                bucket.forEach(element -> arrayNode.add(formatAggregations(element)));
                outputAggregationNode.set(newName, arrayNode);
            } else if (value.isArray()) {
                var arrayNode = objectMapperWithEmpty.createArrayNode();
                value.forEach(element -> arrayNode.add(formatAggregations(element)));
                outputAggregationNode.set(newName, arrayNode);
            } else {
                outputAggregationNode.set(newName, formatAggregations(nodeEntry.getValue()));
            }
        }

        return outputAggregationNode;
    }

    private static JsonNode formatName(JsonNode nodeEntry) {
        var outputAggregationNode = objectMapperWithEmpty.createObjectNode();
        var keyValue = nodeEntry.at("/buckets/0/key");
        outputAggregationNode.set("en", keyValue);
        return outputAggregationNode;
    }

    private static JsonNode formatLabels(JsonNode value) {
        var outputAggregationNode = objectMapperWithEmpty.createObjectNode();

        var iterator = value.fields();
        while (iterator.hasNext()) {
            var nodeEntry = iterator.next();
            var fieldName = nodeEntry.getKey();
            if (fieldName.matches(PATTERN_IS_IGNORE_CASE + "doc.?count.?error.?upper.?bound")) {
                continue;
            }
            if (fieldName.matches(PATTERN_IS_IGNORE_CASE + "sum.?other.?doc.?count")) {
                continue;
            }
            var newName = Optional.ofNullable(AGGREGATION_FIELDS_TO_CHANGE.get(fieldName))
                .orElse(fieldName.replaceFirst(WORD_ENDING_WITH_HASHTAG_REGEX, ""));

            if (newName.equals("count")) {
                continue;
            }
            var keyValue = nodeEntry.getValue().at("/buckets/0/key");
            outputAggregationNode.set(newName, keyValue);
        }
        return outputAggregationNode;
    }

}