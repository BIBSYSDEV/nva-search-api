package no.unit.nva.search.common.bibliography;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonInclude(JsonInclude.Include.NON_NULL)
record SchemaOrgOrganization(
    @JsonProperty("@type") String type, @JsonProperty("@id") String id, String name) {}
