package no.unit.nva.search.resource.response;

import com.fasterxml.jackson.annotation.JsonProperty;

class Affiliation {

    @JsonProperty("id")
    private String id;

    @JsonProperty("type")
    private String type;
}
