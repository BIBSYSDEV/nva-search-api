package no.unit.nva.search.resource.response;

import com.fasterxml.jackson.annotation.JsonProperty;

class Series {

    @JsonProperty("id")
    private String id;

    @JsonProperty("name")
    private String name;
}
