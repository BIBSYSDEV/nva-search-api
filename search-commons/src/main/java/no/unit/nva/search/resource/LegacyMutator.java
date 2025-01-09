package no.unit.nva.search.resource;

import static no.unit.nva.constants.Words.CONTRIBUTORS;
import static no.unit.nva.constants.Words.ENTITY_DESCRIPTION;
import static no.unit.nva.search.resource.Constants.CONTRIBUTORS_PREVIEW;
import static no.unit.nva.search.resource.Constants.GLOBAL_EXCLUDED_FIELDS;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.util.List;
import no.unit.nva.search.common.records.JsonNodeMutator;

public class LegacyMutator implements JsonNodeMutator {

  public static List<String> getExcludedFields() {
    return GLOBAL_EXCLUDED_FIELDS;
  }

  public static List<String> getIncludedFields() {
    return List.of();
  }

  @Override
  public JsonNode transform(JsonNode source) {
    var contributorsPreview = source.path(ENTITY_DESCRIPTION).path(CONTRIBUTORS_PREVIEW);
    if (!contributorsPreview.isMissingNode()) {
      var entityDescription = (ObjectNode) source.path(ENTITY_DESCRIPTION);
      entityDescription.putIfAbsent(CONTRIBUTORS, contributorsPreview);
    }
    return source;
  }
}
