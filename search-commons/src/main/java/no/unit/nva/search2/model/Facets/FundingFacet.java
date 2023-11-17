package no.unit.nva.search2.model.Facets;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.Map;

public class FundingFacet extends Facet {
    
    private final String identifier;
    private final Map<String, String> institutionName;
    
    public FundingFacet(@JsonProperty("cristin_institution_id") String institutionId,
                        @JsonProperty("institution_name") Map<String, String> institutionName) {
        super();
        this.identifier = institutionId;
        this.institutionName = institutionName;
    }
    
    @Override
    public String getKey() {
        return identifier;
    }
    
    @Override
    public Map<String, String> getLabels() {
        return institutionName;
    }
}

