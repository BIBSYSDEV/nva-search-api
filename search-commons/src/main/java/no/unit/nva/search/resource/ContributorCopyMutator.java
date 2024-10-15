package no.unit.nva.search.resource;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import no.unit.nva.search.common.records.JsonNodeMutator;

public class ContributorCopyMutator implements JsonNodeMutator {

    public static final String CONTRIBUTORS_PREVIEW = "contributorsPreview";
    public static final String ENTITY_DESCRIPTION = "entityDescription";
    public static final String CONTRIBUTORS = "contributors";

    @Override
    public JsonNode transform(JsonNode source) {
        var contributorsPreview = source.path(ENTITY_DESCRIPTION).path(CONTRIBUTORS_PREVIEW);
        if (!contributorsPreview.isMissingNode()) {
            var entityDescription = (ObjectNode) source.path(ENTITY_DESCRIPTION);
            entityDescription.put(CONTRIBUTORS, contributorsPreview);
        }
        return source;
    }
}
