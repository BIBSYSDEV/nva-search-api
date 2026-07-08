package no.unit.nva.search.common.bibliography;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonInclude(JsonInclude.Include.NON_NULL)
record SchemaOrgBook(
    @JsonProperty("@type") String type, String name, String isbn, SchemaOrgOrganization publisher)
    implements SchemaOrgContainer {}
