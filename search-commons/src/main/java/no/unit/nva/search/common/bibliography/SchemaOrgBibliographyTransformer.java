package no.unit.nva.search.common.bibliography;

import static no.unit.nva.constants.Words.ABSTRACT;
import static no.unit.nva.constants.Words.CONTRIBUTORS;
import static no.unit.nva.constants.Words.DOI;
import static no.unit.nva.constants.Words.ENTITY_DESCRIPTION;
import static no.unit.nva.constants.Words.HANDLE;
import static no.unit.nva.constants.Words.ID;
import static no.unit.nva.constants.Words.MAIN_TITLE;
import static no.unit.nva.constants.Words.PUBLICATION_DATE;
import static no.unit.nva.constants.Words.REFERENCE;
import static no.unit.nva.constants.Words.TAGS;
import static no.unit.nva.search.common.constant.Functions.jsonPath;
import static nva.commons.core.attempt.Try.attempt;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Collection;
import java.util.List;
import java.util.stream.IntStream;
import no.unit.nva.commons.json.JsonUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class SchemaOrgBibliographyTransformer {

  private static final Logger LOGGER =
      LoggerFactory.getLogger(SchemaOrgBibliographyTransformer.class);

  private static final ObjectMapper MAPPER = JsonUtils.dtoObjectMapper;
  private static final String SCHEMA_ORG_CONTEXT = "https://schema.org";
  private static final String ITEM_LIST_TYPE = "ItemList";
  private static final String LIST_ITEM_TYPE = "ListItem";
  private static final int FIRST_POSITION = 1;

  private SchemaOrgBibliographyTransformer() {} // NO-OP

  public static List<String> getSchemaOrgFields() {
    return List.of(
        ID,
        HANDLE,
        DOI,
        jsonPath(ENTITY_DESCRIPTION, ABSTRACT),
        jsonPath(ENTITY_DESCRIPTION, MAIN_TITLE),
        jsonPath(ENTITY_DESCRIPTION, PUBLICATION_DATE),
        jsonPath(ENTITY_DESCRIPTION, TAGS),
        jsonPath(ENTITY_DESCRIPTION, CONTRIBUTORS),
        jsonPath(ENTITY_DESCRIPTION, REFERENCE));
  }

  public static String transform(Collection<JsonNode> hits, int totalSize) {
    var items = toListItems(hits);
    var itemList = new SchemaOrgItemList(SCHEMA_ORG_CONTEXT, ITEM_LIST_TYPE, totalSize, items);
    return attempt(() -> MAPPER.writeValueAsString(itemList))
        .orElseThrow(
            failure -> {
              LOGGER.error("Failed to serialize schema.org ItemList", failure.getException());
              return new RuntimeException(failure.getException());
            });
  }

  private static List<SchemaOrgListItem> toListItems(Collection<JsonNode> hits) {
    var orderedHits = List.copyOf(hits);
    return IntStream.range(0, orderedHits.size())
        .mapToObj(index -> toListItem(index, orderedHits.get(index)))
        .toList();
  }

  private static SchemaOrgListItem toListItem(int index, JsonNode hit) {
    return new SchemaOrgListItem(
        LIST_ITEM_TYPE, index + FIRST_POSITION, SchemaOrgItemTransformer.transform(hit));
  }
}
