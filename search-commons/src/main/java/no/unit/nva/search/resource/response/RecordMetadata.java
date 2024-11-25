package no.unit.nva.search.resource.response;

import com.fasterxml.jackson.annotation.JsonProperty;

class RecordMetadata {

    @JsonProperty("status")
    private String status;

    @JsonProperty("createdDate")
    private String createdDate;

    @JsonProperty("modifiedDate")
    private String modifiedDate;

    @JsonProperty("publishedDate")
    private String publishedDate;
}
