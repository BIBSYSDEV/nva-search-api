package no.unit.nva.search.common.bibliography;

import static nva.commons.core.attempt.Try.attempt;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Collection;
import no.unit.nva.commons.json.JsonUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class SchemaOrgBibliographyTransformer {

  private static final Logger LOGGER =
      LoggerFactory.getLogger(SchemaOrgBibliographyTransformer.class);

  private static final ObjectMapper MAPPER = JsonUtils.dtoObjectMapper;
  private static final String SCHEMA_ORG_CONTEXT = "https://schema.org";
  private static final String ITEM_LIST_TYPE = "ItemList";

  private SchemaOrgBibliographyTransformer() {} // NO-OP

  public static String transform(Collection<JsonNode> hits, int totalSize) {
    var items = hits.stream().map(SchemaOrgItemTransformer::transform).toList();
    var itemList = new SchemaOrgItemList(SCHEMA_ORG_CONTEXT, ITEM_LIST_TYPE, totalSize, items);
    return attempt(() -> MAPPER.writeValueAsString(itemList))
        .orElseThrow(
            failure -> {
              LOGGER.error("Failed to serialize schema.org ItemList", failure.getException());
              return new RuntimeException(failure.getException());
            });
  }
}
