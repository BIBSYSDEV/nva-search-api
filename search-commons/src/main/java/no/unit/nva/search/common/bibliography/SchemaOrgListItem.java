package no.unit.nva.search.common.bibliography;

import com.fasterxml.jackson.annotation.JsonProperty;

record SchemaOrgListItem(@JsonProperty("@type") String type, int position, SchemaOrgItem item) {}
