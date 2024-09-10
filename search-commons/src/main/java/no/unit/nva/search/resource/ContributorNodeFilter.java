package no.unit.nva.search.resource;

import static nva.commons.core.attempt.Try.attempt;

import com.fasterxml.jackson.core.JsonPointer;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.TextNode;

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
        var pointer = JsonPointer.compile("/entityDescription/contributors");
        var target = source.at(pointer);
        var elements = target.elements();
        while (elements.hasNext()) {
            var element = elements.next();
            if (getVerificationStatus(element).equals("NotVerified") && !hasCountryCode(element)) {
                elements.remove();
            }
        }
        return source;
    }

    private String getVerificationStatus(JsonNode element) {
        return attempt(() -> element.get("identity").get("verificationStatus").asText())
                .orElse((e) -> "");
    }

    private Boolean hasCountryCode(JsonNode element) {
        return attempt(
                        () ->
                                element.path("affiliations")
                                        .findValues("countryCode")
                                        .contains(TextNode.valueOf("NO")))
                .orElse((e) -> Boolean.FALSE);
    }
}
