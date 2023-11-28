package no.unit.nva.search2.common;

import static java.util.Objects.nonNull;
import static no.unit.nva.search.constants.ApplicationConstants.LABELS;
import static no.unit.nva.search.constants.ApplicationConstants.NAME;
import static no.unit.nva.search.constants.ApplicationConstants.objectMapperWithEmpty;
import static no.unit.nva.search2.constant.Patterns.PATTERN_IS_IGNORE_CASE;
import static no.unit.nva.search2.constant.Words.COUNT;
import static no.unit.nva.search2.constant.Words.DOC_COUNT_ERROR_UPPER_BOUND;
import static no.unit.nva.search2.constant.Words.SUM_OTHER_DOC_COUNT;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.JsonNode;
import com.google.common.collect.Streams;
import java.beans.Transient;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import no.unit.nva.search2.common.SwsResponse.HitsInfo.Hit;
import nva.commons.core.JacocoGenerated;
import org.jetbrains.annotations.NotNull;

public record SwsResponse(
    int took,
    boolean timed_out,
    ShardsInfo _shards,
    HitsInfo hits,
    JsonNode aggregations) {

    public static final String BUCKETS_0_KEY = "/buckets/0/key";
    public static final String ID_BUCKETS = "/id/buckets";
    public static final String BUCKETS = "buckets";

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

    public static JsonNode formatAggregations(JsonNode aggregations) {
        var outputAggregationNode = objectMapperWithEmpty.createObjectNode();

        Streams.stream(aggregations.fields())
            .filter(SwsResponse::ignoreDocCountErrors)
            .filter(SwsResponse::ignoreSumOtherDoc)
            .map(SwsResponse::getJsonNodeEntry)
            .forEach(entry -> {
                if (LABELS.equals(entry.getKey())) {
                    outputAggregationNode.set(entry.getKey(), formatLabels(entry.getValue()));
                } else if (NAME.equals(entry.getKey())) {
                    outputAggregationNode.set(LABELS, formatName(entry.getValue()));
                } else if (entry.getValue().isValueNode()) {
                    outputAggregationNode.set(entry.getKey(), entry.getValue());
                } else if (entry.getValue().isArray()) {
                    var arrayNode = objectMapperWithEmpty.createArrayNode();
                    entry.getValue().forEach(element -> arrayNode.add(formatAggregations(element)));
                    outputAggregationNode.set(entry.getKey(), arrayNode);
                } else {
                    outputAggregationNode.set(entry.getKey(), formatAggregations(entry.getValue()));
                }
            });
        return outputAggregationNode;
    }

    @NotNull
    private static Entry<String, JsonNode> getJsonNodeEntry(Entry<String, JsonNode> entry) {
        return Map.entry(getNormalizedFieldName(entry.getKey()), getBucketOrValue(entry.getValue()));
    }

    private static boolean ignoreSumOtherDoc(Entry<String, JsonNode> item) {
        return !item.getKey().matches(PATTERN_IS_IGNORE_CASE + SUM_OTHER_DOC_COUNT);
    }

    private static boolean ignoreDocCountErrors(Entry<String, JsonNode> item) {
        return !item.getKey().matches(PATTERN_IS_IGNORE_CASE + DOC_COUNT_ERROR_UPPER_BOUND);
    }

    private static JsonNode getBucketOrValue(JsonNode node) {
        if (node.at(ID_BUCKETS).isArray()) {
            return node.at(ID_BUCKETS);
        }
        if (node.has(BUCKETS)) {
            return node.at("/buckets");
        }
        return node;
    }

    private static JsonNode formatName(JsonNode nodeEntry) {
        var outputAggregationNode = objectMapperWithEmpty.createObjectNode();
        var keyValue = nodeEntry.at(BUCKETS_0_KEY);
        outputAggregationNode.set("en", keyValue);
        return outputAggregationNode;
    }

    private static JsonNode formatLabels(JsonNode value) {
        var outputAggregationNode = objectMapperWithEmpty.createObjectNode();
        value.fields().forEachRemaining(node -> {
            var fieldName = node.getKey();
            if (fieldName.matches(PATTERN_IS_IGNORE_CASE + DOC_COUNT_ERROR_UPPER_BOUND)) {
                return;
            }
            if (fieldName.matches(PATTERN_IS_IGNORE_CASE + SUM_OTHER_DOC_COUNT)) {
                return;
            }
            var newName = getNormalizedFieldName(fieldName);

            if (COUNT.equals(newName)) {
                return;
            }
            var keyValue = node.getValue().at(BUCKETS_0_KEY);
            outputAggregationNode.set(newName, keyValue);
        });

        return outputAggregationNode;
    }

    @NotNull
    private static String getNormalizedFieldName(String fieldName) {
        return Optional.ofNullable(Constants.AGGREGATION_FIELDS_TO_CHANGE.get(fieldName))
            .orElse(fieldName.replaceFirst(Constants.WORD_ENDING_WITH_HASHTAG_REGEX, ""));
    }

    static final class Constants {

        public static final String WORD_ENDING_WITH_HASHTAG_REGEX = "[A-za-z0-9]*#";

        private static final Map<String, String> AGGREGATION_FIELDS_TO_CHANGE = Map.of(
            "docCount", COUNT,
            "doc_count", COUNT);
    }
}
