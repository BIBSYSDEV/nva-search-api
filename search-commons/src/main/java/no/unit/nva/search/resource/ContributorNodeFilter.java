package no.unit.nva.search.resource;

import static nva.commons.core.attempt.Try.attempt;

import com.fasterxml.jackson.core.JsonPointer;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.TextNode;

import no.unit.nva.constants.Words;
import no.unit.nva.search.common.records.Midas;

import nva.commons.core.JacocoGenerated;

public class ContributorNodeFilter implements Midas {

    @JacocoGenerated
    ContributorNodeFilter() {}

    public static ContributorNodeFilter verifiedOrNorwegian() {
        return new ContributorNodeFilter();
    }

    @Override
    public JsonNode transform(JsonNode source) {
        var pointer = JsonPointer.compile(Words.ENTITY_DESCRIPTION_CONTRIBUTORS_PATH);
        var target = source.at(pointer);
        var elements = target.elements();
        while (elements.hasNext()) {
            var element = elements.next();
            if (hasVerificationStatus(element) && !hasCountryCode(element)) {
                elements.remove();
            }
        }
        return source;
    }

    private Boolean hasVerificationStatus(JsonNode element) {
        return attempt(
                        () ->
                                Words.NOT_VERIFIED.equals(
                                        element.get(Words.IDENTITY)
                                                .get(Words.VERIFICATION_STATUS)
                                                .asText()))
                .orElse((e) -> Boolean.FALSE);
    }

    private Boolean hasCountryCode(JsonNode element) {
        return attempt(
                        () ->
                                element.path(Words.AFFILIATIONS)
                                        .findValues(Words.COUNTRY_CODE)
                                        .contains(TextNode.valueOf(Words.NO)))
                .orElse((e) -> Boolean.FALSE);
    }
}
