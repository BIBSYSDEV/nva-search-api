package no.unit.nva.search.common.bibliography;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonInclude(JsonInclude.Include.NON_NULL)
record SchemaOrgPublicationVolume(
    @JsonProperty("@type") String type, String volumeNumber, SchemaOrgContainer isPartOf)
    implements SchemaOrgContainer {}
