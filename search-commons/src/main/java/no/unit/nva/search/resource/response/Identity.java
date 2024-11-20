package no.unit.nva.search.resource.response;

import com.fasterxml.jackson.annotation.JsonProperty;

class Identity {

    @JsonProperty("id")
    private String id;

    @JsonProperty("type")
    private String type;

    @JsonProperty("name")
    private String name;
}
