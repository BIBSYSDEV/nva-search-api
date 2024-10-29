package no.unit.nva.search.resource.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

public class OtherIdentifiers {

    @JsonProperty("scopus")
    private List<String> scopus;

    @JsonProperty("scopus")
    private List<String> cristin;

    @JsonProperty("scopus")
    private List<String> handle;

    @JsonProperty("scopus")
    private List<String> issn;

    @JsonProperty("scopus")
    private List<String> isbn;

}
