package no.unit.nva.search.common.bibliography;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

@JsonInclude(JsonInclude.Include.NON_NULL)
record SchemaOrgPerson(
    @JsonProperty("@type") String type,
    @JsonProperty("@id") String id,
    String sameAs,
    String name,
    List<SchemaOrgOrganization> affiliation) {}
