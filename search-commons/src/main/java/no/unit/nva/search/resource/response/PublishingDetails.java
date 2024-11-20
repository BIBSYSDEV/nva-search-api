package no.unit.nva.search.resource.response;

import com.fasterxml.jackson.annotation.JsonProperty;

class PublishingDetails {

    @JsonProperty("id")
    private String id;

    @JsonProperty("type")
    private String type;

    @JsonProperty("series")
    private Series series;

    @JsonProperty("name")
    private String name;

    @JsonProperty("doi")
    private String doi;
}
