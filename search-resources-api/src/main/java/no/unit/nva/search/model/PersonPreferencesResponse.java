package no.unit.nva.search.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;
import no.unit.nva.commons.json.JsonSerializable;

public class PersonPreferencesResponse implements JsonSerializable {

    public static final String PROMOTED_PUBLICATIONS = "promotedPublications";
    @JsonProperty(PROMOTED_PUBLICATIONS)
    private final List<String> promotedPublications;

    @JsonCreator
    public PersonPreferencesResponse(@JsonProperty(PROMOTED_PUBLICATIONS) List<String> promotedPublications) {
        this.promotedPublications = promotedPublications;
    }

    public List<String> getPromotedPublications() {
        return promotedPublications;
    }

    @Override
    public String toString() {
        return this.toJsonString();
    }
}
