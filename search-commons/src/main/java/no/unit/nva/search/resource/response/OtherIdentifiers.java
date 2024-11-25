package no.unit.nva.search.resource.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

public class OtherIdentifiers {

    @JsonProperty("scopus")
    private List<String> scopus;

    @JsonProperty("cristin")
    private List<String> cristin;

    @JsonProperty("handle")
    private List<String> handle;

    @JsonProperty("issn")
    private List<String> issn;

    @JsonProperty("isbn")
    private List<String> isbn;

}
