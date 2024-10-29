package no.unit.nva.search.resource.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

class Contributor {

    @JsonProperty("affiliations")
    private List<Affiliation> affiliations;

    @JsonProperty("correspondingAuthor")
    private boolean correspondingAuthor;

    @JsonProperty("identity")
    private Identity identity;

    @JsonProperty("role")
    private String role;

    @JsonProperty("sequence")
    private int sequence;

    @JsonProperty("type")
    private String type;
}
