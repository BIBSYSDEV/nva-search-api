package no.unit.nva.search.common.bibliography;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

record SchemaOrgItemList(
    @JsonProperty("@context") String context,
    @JsonProperty("@type") String type,
    int numberOfItems,
    List<SchemaOrgItem> itemListElement) {}
