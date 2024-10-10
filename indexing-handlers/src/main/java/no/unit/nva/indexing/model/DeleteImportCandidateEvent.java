package no.unit.nva.indexing.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import no.unit.nva.commons.json.JsonSerializable;
import no.unit.nva.identifiers.SortableIdentifier;

public record DeleteImportCandidateEvent(String topic, SortableIdentifier identifier) implements JsonSerializable {

    public static final String EVENT_TOPIC = "ImportCandidates.ExpandedEntry.Deleted";
    public static final String TOPIC = "topic";
    public static final String IDENTIFIER = "identifier";

    @JsonCreator
    public DeleteImportCandidateEvent(
            @JsonProperty(TOPIC) String topic,
            @JsonProperty(IDENTIFIER) SortableIdentifier identifier) {
        this.topic = topic;
        this.identifier = identifier;
    }
}
