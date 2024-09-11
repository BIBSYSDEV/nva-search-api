package no.unit.nva.search.resource;

import static no.unit.nva.constants.Words.AFFILIATIONS;
import static no.unit.nva.constants.Words.COUNTRY_CODE;
import static no.unit.nva.constants.Words.ENTITY_DESCRIPTION_CONTRIBUTORS_PATH;
import static no.unit.nva.constants.Words.IDENTITY;
import static no.unit.nva.constants.Words.NO;
import static no.unit.nva.constants.Words.VERIFICATION_STATUS;
import static no.unit.nva.constants.Words.VERIFIED;

import static nva.commons.core.attempt.Try.attempt;

import com.fasterxml.jackson.core.JsonPointer;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.TextNode;

import no.unit.nva.search.common.records.Midas;

import nva.commons.core.JacocoGenerated;

public class ContributorNodeFilter implements Midas {

    // (package private)
    static final int FIRST_FIVE_CONTRIBUTORS = 5;
    static final JsonPointer CONTRIBUTORS_PATH_POINTER = JsonPointer.compile(ENTITY_DESCRIPTION_CONTRIBUTORS_PATH);

    @JacocoGenerated
    ContributorNodeFilter() {}

    public static ContributorNodeFilter verifiedOrNorwegian() {
        return new ContributorNodeFilter();
    }

    @Override
    public JsonNode transform(JsonNode source) {
        var target = source.at(CONTRIBUTORS_PATH_POINTER);
        var elements = target.elements();
        while (elements.hasNext()) {
            var element = elements.next();
            if (deleteAfter(element, FIRST_FIVE_CONTRIBUTORS)
                    && isNotVerified(element)
                    && hasNoNorwegianBasedContributor(element)) {
                elements.remove();
            }
        }
        return source;
    }

    private Boolean isNotVerified(JsonNode element) {
        return attempt(
            () ->!VERIFIED.equals(element.get(IDENTITY).get(VERIFICATION_STATUS).asText()))
                .orElse((e) -> Boolean.TRUE);
    }

    private Boolean hasNoNorwegianBasedContributor(JsonNode element) {
        return attempt(
                        () ->
                                !element.path(AFFILIATIONS)
                                        .findValues(COUNTRY_CODE)
                                        .contains(TextNode.valueOf(NO)))
                .orElse((e) -> Boolean.TRUE);
    }

    private Boolean deleteAfter(JsonNode element, int count) {
        return attempt(() -> count < element.get(Constants.SEQUENCE).asInt()).orElse((e) -> Boolean.TRUE);
    }
}
