package no.unit.nva.search.resource.response;

import com.fasterxml.jackson.annotation.JsonProperty;

public class PublicationDate {

    @JsonProperty("year")
    private String year;
    @JsonProperty("month")
    private String month;

    @JsonProperty("day")
    private String day;

}
