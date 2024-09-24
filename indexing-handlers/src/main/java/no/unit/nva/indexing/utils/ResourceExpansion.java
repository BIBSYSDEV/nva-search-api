package no.unit.nva.indexing.utils;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.IntNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.util.Spliterators;
import java.util.stream.StreamSupport;

public class ResourceExpansion {

    public static final String ENTITY_DESCRIPTION = "entityDescription";
    public static final String CONTRIBUTORS = "contributors";
    public static final String CONTRIBUTORS_COUNT = "contributorsCount";
    private static final String CONTRIBUTORS_PROMOTED = "contributorsPromoted";
    public static final String IDENTITY = "identity";
    public static final String VERIFICATION_STATUS = "verificationStatus";
    public static final String VERIFIED = "Verified";
    public static final String SEQUENCE = "sequence";

    public static JsonNode expandContributorCount(JsonNode jsonNode) {
        var contributorNode = jsonNode.path(ENTITY_DESCRIPTION).path(CONTRIBUTORS);
        if (!contributorNode.isMissingNode()) {
            ((ObjectNode) jsonNode.get(ENTITY_DESCRIPTION))
                    .set(CONTRIBUTORS_COUNT, IntNode.valueOf(contributorNode.size()));
        }
        return jsonNode;
    }

    private static void expandPromotedContributors(JsonNode jsonNode) {
        var contributorNode = jsonNode.path(ENTITY_DESCRIPTION).path(CONTRIBUTORS);
        if (!contributorNode.isMissingNode()) {
            var contributorList =
                    StreamSupport.stream(
                                    Spliterators.spliteratorUnknownSize(
                                            contributorNode.elements(), 0),
                                    false)
                            .toList();

            var contributorsPromoted =
                    contributorList.stream()
                            .sorted(ResourceExpansion::sortBySequence)
                            .sorted(ResourceExpansion::sortByPromoted)
                            .limit(10)
                            .toList();

            ArrayNode arrayNode = new ArrayNode(JsonNodeFactory.instance);
            arrayNode.addAll(contributorsPromoted);

            ((ObjectNode) jsonNode.get(ENTITY_DESCRIPTION)).set(CONTRIBUTORS_PROMOTED, arrayNode);
        }
    }

    private static int sortBySequence(JsonNode contributor1, JsonNode contributor2) {
        var contributor1seq = contributor1.get(SEQUENCE);
        var contributor2seq = contributor2.get(SEQUENCE);

        if (contributor1seq.isMissingNode() && contributor2seq.isMissingNode()) {
            return 0;
        }

        if (contributor1seq.isMissingNode()) {
            return 1;
        }

        if (contributor2seq.isMissingNode()) {
            return -1;
        }

        return contributor1seq.intValue() - contributor2seq.intValue();
    }

    private static int sortByPromoted(JsonNode contributor1, JsonNode contributor2) {
        var contributor1verification = isContributorVerified(contributor1);
        var contributor2verification = isContributorVerified(contributor2);

        if (contributor1verification && !contributor2verification) {
            return -1;
        }

        if (!contributor1verification && contributor2verification) {
            return 1;
        }

        return 0;
    }

    private static boolean isContributorVerified(JsonNode contributor) {
        var verificationStatus = contributor.path(IDENTITY).path(VERIFICATION_STATUS);
        if (verificationStatus.isMissingNode()) {
            return false;
        }
        return verificationStatus.textValue().equals(VERIFIED);
    }

    public static JsonNode expand(JsonNode jsonNode) {
        expandContributorCount(jsonNode);
        expandPromotedContributors(jsonNode);
        return jsonNode;
    }
}
