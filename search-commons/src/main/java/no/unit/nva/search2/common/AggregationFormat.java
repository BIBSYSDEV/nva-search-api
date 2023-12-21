package no.unit.nva.search2.common;

import static no.unit.nva.search2.constant.Patterns.PATTERN_IS_WORD_ENDING_WITH_HASHTAG;
import static no.unit.nva.search2.constant.Words.BUCKETS;
import static no.unit.nva.search2.constant.Words.COUNT;
import static no.unit.nva.search2.constant.Words.ENGLISH_CODE;
import static no.unit.nva.search2.constant.Words.ID;
import static no.unit.nva.search2.constant.Words.KEY;
import static no.unit.nva.search2.constant.Words.LABELS;
import static no.unit.nva.search2.constant.Words.NAME;
import static no.unit.nva.search2.constant.Words.SLASH;
import static no.unit.nva.search2.constant.Words.ZERO;
import static nva.commons.core.StringUtils.EMPTY_STRING;
import com.fasterxml.jackson.databind.JsonNode;
import com.google.common.collect.Streams;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import no.unit.nva.commons.json.JsonUtils;
import nva.commons.core.JacocoGenerated;

public final class AggregationFormat {
    public JsonNode apply(JsonNode aggregations) {

        var outputAggregationNode =  JsonUtils.dtoObjectMapper.createObjectNode();

        Streams.stream(aggregations.fields())
            .map(AggregationFormat::getJsonNodeEntry)
            .forEach(entry -> {
                if (keyIsLabel(entry)) {
                    outputAggregationNode.set(entry.getKey(), formatLabels(entry.getValue()));
                } else if (keyIsName(entry)) {
                    outputAggregationNode.set(LABELS, formatName(entry.getValue()));
                } else if (isValueNode(entry)) {
                    outputAggregationNode.set(entry.getKey(), entry.getValue());
                } else if (isArrayNode(entry)) {
                    var arrayNode = JsonUtils.dtoObjectMapper.createArrayNode();
                    entry.getValue().forEach(element -> arrayNode.add(apply(element)));
                    outputAggregationNode.set(entry.getKey(), arrayNode);
                } else {
                    outputAggregationNode.set(entry.getKey(), apply(entry.getValue()));
                }
            });
        return outputAggregationNode;
    }

    private boolean isArrayNode(Entry<String, JsonNode> entry) {
        return entry.getValue().isArray();
    }

    private boolean isValueNode(Entry<String, JsonNode> entry) {
        return entry.getValue().isValueNode();
    }

    private static boolean keyIsName(Entry<String, JsonNode> entry) {
        return NAME.equals(entry.getKey());
    }

    private static boolean keyIsLabel(Entry<String, JsonNode> entry) {
        return LABELS.equals(entry.getKey());
    }

    private static Map.Entry<String, JsonNode> getJsonNodeEntry(Map.Entry<String, JsonNode> entry) {
        return Map.entry(getNormalizedFieldName(entry.getKey()), getBucketOrValue(entry.getValue()));
    }

    private static Map.Entry<String, JsonNode> getNormalizedJsonNodeEntry(Map.Entry<String, JsonNode> entry) {
        return Map.entry(getNormalizedFieldName(entry.getKey()), entry.getValue());
    }

    private static JsonNode getBucketOrValue(JsonNode node) {
        if (node.at(Constants.ID_BUCKETS).isArray()) {
            return node.at(Constants.ID_BUCKETS);
        }
        if (node.has(BUCKETS)) {
            return node.at(Constants.BUCKETS_PTR);
        }
        return node;
    }

    private static JsonNode formatName(JsonNode nodeEntry) {
        var outputAggregationNode = JsonUtils.dtoObjectMapper.createObjectNode();
        var keyValue = nodeEntry.at(Constants.BUCKETS_KEY_PTR);
        if (keyValue.isEmpty()) {
            keyValue = nodeEntry.at(Constants.KEY_PTR);
        }
        outputAggregationNode.set(ENGLISH_CODE, keyValue);
        return outputAggregationNode;
    }

    private static JsonNode formatLabels(JsonNode value) {
        var outputAggregationNode = JsonUtils.dtoObjectMapper.createObjectNode();

        Streams.stream(value.fields())
            .map(AggregationFormat::getNormalizedJsonNodeEntry)
            .filter(entry -> !COUNT.equals(entry.getKey()))
            .forEach(node -> {
                var keyValue = node.getValue().at(Constants.BUCKETS_KEY_PTR);
                outputAggregationNode.set(node.getKey(), keyValue);
            });
        return outputAggregationNode;
    }

    private static String getNormalizedFieldName(String fieldName) {
        return Optional.ofNullable(Constants.AGGREGATION_FIELDS_TO_CHANGE.get(fieldName))
            .orElse(fieldName.replaceFirst(PATTERN_IS_WORD_ENDING_WITH_HASHTAG, EMPTY_STRING));
    }

    @JacocoGenerated // Class is never instantiated, just constants are used
    static final class Constants {
        public static final String BUCKETS_PTR = SLASH + BUCKETS;
        public static final String KEY_PTR = SLASH + ZERO + SLASH + KEY;
        public static final String BUCKETS_KEY_PTR = SLASH + BUCKETS + KEY_PTR;
        public static final String ID_BUCKETS = SLASH + ID + SLASH + BUCKETS;
        private static final Map<String, String> AGGREGATION_FIELDS_TO_CHANGE =
            Map.of(
            "docCount", COUNT,
            "doc_count", COUNT
            );
    }

}
