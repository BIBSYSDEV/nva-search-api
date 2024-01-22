package no.unit.nva.search2.common;

import static no.unit.nva.search2.constant.Patterns.PATTERN_IS_WORD_ENDING_WITH_HASHTAG;
import static no.unit.nva.search2.constant.Words.BUCKETS;
import static no.unit.nva.search2.constant.Words.CONTRIBUTOR;
import static no.unit.nva.search2.constant.Words.ENGLISH_CODE;
import static no.unit.nva.search2.constant.Words.FUNDING_SOURCE;
import static no.unit.nva.search2.constant.Words.HAS_FILE;
import static no.unit.nva.search2.constant.Words.KEY;
import static no.unit.nva.search2.constant.Words.LABELS;
import static no.unit.nva.search2.constant.Words.LICENSE;
import static no.unit.nva.search2.constant.Words.NAME;
import static no.unit.nva.search2.constant.Words.PUBLISHER;
import static no.unit.nva.search2.constant.Words.SERIES;
import static no.unit.nva.search2.constant.Words.SLASH;
import static no.unit.nva.search2.constant.Words.STATUS;
import static no.unit.nva.search2.constant.Words.TOP_LEVEL_ORGANIZATION;
import static no.unit.nva.search2.constant.Words.TYPE;
import static no.unit.nva.search2.constant.Words.ZERO;
import static nva.commons.core.StringUtils.EMPTY_STRING;
import com.fasterxml.jackson.databind.JsonNode;
import com.google.common.collect.Streams;
import java.util.Map;
import java.util.Map.Entry;
import java.util.stream.Stream;
import no.unit.nva.commons.json.JsonUtils;
import nva.commons.core.JacocoGenerated;

public final class AggregationFormat {

    @JacocoGenerated
    public AggregationFormat() {
    }

    public static JsonNode apply(JsonNode aggregations) {

        var objectNode = JsonUtils.dtoObjectMapper.createObjectNode();

        getAggregationFieldStreams(aggregations)
            .map(AggregationFormat::getJsonNodeEntry)
            .forEach(item -> objectNode.set(item.getKey(), fixNodes(item.getValue())));
        return objectNode;
    }

    private static JsonNode fixNodes(JsonNode node) {
        if (node.isArray()) {
            var arrayNode = JsonUtils.dtoObjectMapper.createArrayNode();
            node.forEach(element -> arrayNode.add(fixNodes(element)));
            return arrayNode;
        } else {
            var outputAggregationNode = JsonUtils.dtoObjectMapper.createObjectNode();
            node.fields().forEachRemaining(entry -> {
                if (keyIsLabel(entry)) {
                    outputAggregationNode.set(entry.getKey(), formatLabels(entry.getValue()));
                } else if (keyIsName(entry)) {
                    outputAggregationNode.set(LABELS, formatName(entry.getValue()));
                } else {
                    outputAggregationNode.set(entry.getKey(), entry.getValue());
                }
            });
            return outputAggregationNode.isEmpty()
                ? JsonUtils.dtoObjectMapper.createArrayNode()
                : outputAggregationNode;
        }
    }

    private static Stream<Entry<String, JsonNode>> getAggregationFieldStreams(JsonNode aggregations) {
        return Constants.facetPaths.entrySet().stream().map(
            entry -> Map.entry(entry.getKey(), aggregations.at(entry.getValue()))
        );
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
            .filter(entry -> !Constants.DOC_COUNT.equals(entry.getKey()))
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

        public static final Map<String, String> facetPaths = Map.of(
            TYPE, "/filter/entityDescription/reference/publicationInstance/type",
            SERIES, "/filter/entityDescription/reference/publicationContext/series",
            STATUS, "/filter/status",
            LICENSE, "/filter/associatedArtifacts/license",
            HAS_FILE, "/filter/associatedArtifacts/hasFile",
            PUBLISHER, "/filter/entityDescription/reference/publicationContext/publisher",
            CONTRIBUTOR, "/filter/entityDescription/contributor/id",
            FUNDING_SOURCE, "/filter/fundings/id",
            TOP_LEVEL_ORGANIZATION, "/filter/topLevelOrganizations/id"
        );

        public static final String DOC_COUNT = "doc_count";
        public static final String BUCKETS_PTR = SLASH + BUCKETS;
        public static final String KEY_PTR = SLASH + ZERO + SLASH + KEY;
        public static final String BUCKETS_KEY_PTR = SLASH + BUCKETS + KEY_PTR;
    }

}
