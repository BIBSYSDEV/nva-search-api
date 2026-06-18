package no.unit.nva.search.common.bibliography;

import static nva.commons.core.attempt.Try.attempt;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import java.util.Collection;
import no.unit.nva.commons.json.JsonUtils;

public final class SchemaOrgBibliographyTransformer {

  private static final ObjectMapper MAPPER =
      JsonUtils.dtoObjectMapper.copy().disable(SerializationFeature.INDENT_OUTPUT);

  private SchemaOrgBibliographyTransformer() {}

  public static String transform(Collection<JsonNode> hits, int totalSize) {
    var items = hits.stream().map(SchemaOrgItemTransformer::transform).toList();
    var itemList = new SchemaOrgItemList("https://schema.org", "ItemList", totalSize, items);
    var serialized = attempt(() -> MAPPER.writeValueAsString(itemList));
    return serialized.orElseThrow();
  }
}
