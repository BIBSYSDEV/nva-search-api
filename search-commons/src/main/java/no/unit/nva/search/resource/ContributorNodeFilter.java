package no.unit.nva.search.resource;

import static no.unit.nva.constants.Words.AFFILIATIONS;
import static no.unit.nva.constants.Words.COUNTRY_CODE;
import static no.unit.nva.constants.Words.DOT;
import static no.unit.nva.constants.Words.ENTITY_DESCRIPTION_CONTRIBUTORS_PATH;
import static no.unit.nva.constants.Words.IDENTITY;
import static no.unit.nva.constants.Words.NO;
import static no.unit.nva.constants.Words.VERIFICATION_STATUS;
import static no.unit.nva.constants.Words.VERIFIED;

import com.fasterxml.jackson.core.JsonPointer;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.TextNode;

import no.unit.nva.search.common.records.JsonNodeMutator;

public final class ContributorNodeFilter implements JsonNodeMutator {

    static final int MINIMUM_INCLUDED_CONTRIBUTORS = 5;
    static final JsonPointer CONTRIBUTORS_PATH_POINTER =
            JsonPointer.compile(ENTITY_DESCRIPTION_CONTRIBUTORS_PATH);

    private ContributorNodeFilter() {}

    public static ContributorNodeFilter verifiedOrNorwegian() {
        return new ContributorNodeFilter();
    }

    @Override
    public JsonNode transform(JsonNode source) {
        var target = source.at(CONTRIBUTORS_PATH_POINTER);
        var elements = target.elements();
        while (elements.hasNext()) {
            var element = elements.next();
            if (shouldRemoveContributor(element)) {
                elements.remove();
            }
        }
        return source;
    }

    private boolean shouldRemoveContributor(JsonNode element) {
        return deleteAfterMinimumSequenceNo(element)
            && isNotVerified(element)
            && hasNoNorwegianBasedContributor(element);
    }

    private boolean isNotVerified(JsonNode element) {
        return !VERIFIED.equals(element.path(IDENTITY + DOT + VERIFICATION_STATUS).asText());
    }

    private boolean hasNoNorwegianBasedContributor(JsonNode element) {
        return !element.path(AFFILIATIONS).findValues(COUNTRY_CODE).contains(TextNode.valueOf(NO));
    }

    private boolean deleteAfterMinimumSequenceNo(JsonNode element) {
        var sequence = element.path(Constants.SEQUENCE);
        return sequence.isMissingNode() || MINIMUM_INCLUDED_CONTRIBUTORS < sequence.asInt();
    }
}
