package no.unit.nva.search.models;

import com.fasterxml.jackson.annotation.JsonProperty;
import no.unit.nva.identifiers.SortableIdentifier;

public record EventConsumptionAttributes(
    @JsonProperty(INDEX_FIELD) String index,
    @JsonProperty(DOCUMENT_IDENTIFIER) SortableIdentifier documentIdentifier
) {

    public static final String INDEX_FIELD = "index";
    public static final String DOCUMENT_IDENTIFIER = "documentIdentifier";

    @Override
    public String index() {
        return index;
    }

    @Override
    public SortableIdentifier documentIdentifier() {
        return documentIdentifier;
    }

}
