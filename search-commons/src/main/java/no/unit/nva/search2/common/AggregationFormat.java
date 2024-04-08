package no.unit.nva.search2.common;

import static java.util.Objects.nonNull;
import static no.unit.nva.search2.common.AggregationFormat.Constants.DOC_COUNT;
import static no.unit.nva.search2.common.constant.Patterns.PATTERN_IS_WORD_ENDING_WITH_HASHTAG;
import static no.unit.nva.search2.common.constant.Words.BUCKETS;
import static no.unit.nva.search2.common.constant.Words.ENGLISH_CODE;
import static no.unit.nva.search2.common.constant.Words.KEY;
import static no.unit.nva.search2.common.constant.Words.LABELS;
import static no.unit.nva.search2.common.constant.Words.NAME;
import static no.unit.nva.search2.common.constant.Words.SLASH;
import static no.unit.nva.search2.common.constant.Words.VALUE;
import static no.unit.nva.search2.common.constant.Words.ZERO;
import static nva.commons.core.StringUtils.EMPTY_STRING;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.collect.Streams;
import java.util.ArrayList;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import java.util.stream.Stream;
import no.unit.nva.commons.json.JsonUtils;
import nva.commons.core.JacocoGenerated;

public final class AggregationFormat {

    public static final String UNIQUE_PUBLICATIONS = "unique_publications";

    @JacocoGenerated
    public AggregationFormat() {
    }

    public static JsonNode apply(JsonNode aggregations, Map<String, String> definitions) {
        var objectNode = JsonUtils.dtoObjectMapper.createObjectNode();
        if (nonNull(aggregations)) {
            getAggregationFieldStreams(aggregations, definitions)
                .map(AggregationFormat::getJsonNodeEntry)
                .forEach(item -> objectNode.set(item.getKey(), fixNodes(item, item.getValue())));
        }
        combineNotificationAggregations(objectNode);
        return objectNode;
    }


    private static void combineNotificationAggregations(ObjectNode jsonNode) {
        var notifications = JsonUtils.dtoObjectMapper.createObjectNode().arrayNode();
        var keysToRemove = new ArrayList<String>();
        for (var field : jsonNode.properties()) {
            var fieldName = field.getKey();
            if (isNotificationAggregation(field)) {
                JsonNode value = field.getValue();
                ((ObjectNode) value).put("key", fieldName);
                notifications.add(value);
                keysToRemove.add(fieldName);
            }
        }
        keysToRemove.forEach(jsonNode::remove);
        jsonNode.put("notifications", notifications);
    }

    private static boolean isNotificationAggregation(Entry<String, JsonNode> field) {
        return field.getKey().toLowerCase(Locale.getDefault()).contains("notification") && field.getValue().isObject();
    }

    private static JsonNode fixNodes(Entry<String, JsonNode> item, JsonNode node) {
        if (node.isArray()) {
            var arrayNode = JsonUtils.dtoObjectMapper.createArrayNode();
            node.forEach(element -> arrayNode.add(fixNodes(item, element)));
            return arrayNode;
        } else {
            var outputAggregationNode = JsonUtils.dtoObjectMapper.createObjectNode();
            node.fields().forEachRemaining(entry -> extractKeyAndLabels(entry, outputAggregationNode));
            return outputAggregationNode.isEmpty()
                ? JsonUtils.dtoObjectMapper.createArrayNode()
                : outputAggregationNode;
        }
    }

    private static void extractKeyAndLabels(Entry<String, JsonNode> entry, ObjectNode outputAggregationNode) {
        if (keyIsLabel(entry)) {
            outputAggregationNode.set(entry.getKey(), formatLabels(entry.getValue()));
        } else if (keyIsName(entry)) {
            outputAggregationNode.set(LABELS, formatName(entry.getValue()));
        } else if (rootHasUniquePublicationsCount(entry)) {
            outputAggregationNode.set(DOC_COUNT, entry.getValue().get(UNIQUE_PUBLICATIONS).get(VALUE));
        } else {
            outputAggregationNode.set(entry.getKey(), entry.getValue());
        }
    }

    private static boolean rootHasUniquePublicationsCount(Entry<String, JsonNode> entry) {
        return nonNull(entry.getValue().get(UNIQUE_PUBLICATIONS))
            && nonNull(entry.getValue().get(UNIQUE_PUBLICATIONS).get(VALUE));
    }

    private static Stream<Entry<String, JsonNode>> getAggregationFieldStreams(JsonNode aggregations,
                                                                              Map<String, String> definitions) {
        return definitions
            .entrySet().stream()
            .map(entry -> Map.entry(entry.getKey(), aggregations.at(entry.getValue()))
            );
    }

    private static boolean keyIsName(Entry<String, JsonNode> entry) {
        return NAME.equals(entry.getKey());
    }

    private static boolean keyIsLabel(Entry<String, JsonNode> entry) {
        return LABELS.equals(entry.getKey());
    }

    private static Map.Entry<String, JsonNode> getJsonNodeEntry(Map.Entry<String, JsonNode> entry) {
        return Map.entry(getNormalizedFieldName(entry.getKey()),
            getBucketOrValue(entry.getValue()));
    }

    private static Map.Entry<String, JsonNode> getNormalizedJsonNodeEntry(Map.Entry<String, JsonNode> entry) {
        return Map.entry(getNormalizedFieldName(entry.getKey()), entry.getValue());
    }

    private static JsonNode getBucketOrValue(JsonNode node) {
        if (node.has(BUCKETS)) {
            return node.at(Constants.BUCKETS_PTR);
        }
        return node;
    }

    private static JsonNode formatName(JsonNode nodeEntry) {
        var outputAggregationNode = JsonUtils.dtoObjectMapper.createObjectNode();
        var keyValue = nodeEntry.at(Constants.BUCKETS_KEY_PTR);
        outputAggregationNode.set(ENGLISH_CODE, keyValue);
        return outputAggregationNode;
    }

    private static JsonNode formatLabels(JsonNode value) {
        var outputAggregationNode = JsonUtils.dtoObjectMapper.createObjectNode();

        Streams.stream(value.fields())
            .map(AggregationFormat::getNormalizedJsonNodeEntry)
            .filter(entry -> !DOC_COUNT.equals(entry.getKey()))
            .forEach(node -> {
                var keyValue = node.getValue().at(Constants.BUCKETS_KEY_PTR);
                outputAggregationNode.set(node.getKey(), keyValue);
            });
        return outputAggregationNode;
    }

    private static String getNormalizedFieldName(String fieldName) {
        return fieldName.replaceFirst(PATTERN_IS_WORD_ENDING_WITH_HASHTAG, EMPTY_STRING);
    }

    @JacocoGenerated
    static final class Constants {
        public static final String DOC_COUNT = "doc_count";
        public static final String BUCKETS_PTR = SLASH + BUCKETS;
        public static final String KEY_PTR = SLASH + ZERO + SLASH + KEY;
        public static final String BUCKETS_KEY_PTR = SLASH + BUCKETS + KEY_PTR;
    }
}