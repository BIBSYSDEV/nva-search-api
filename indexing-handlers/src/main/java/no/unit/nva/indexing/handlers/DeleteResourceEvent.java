package no.unit.nva.indexing.handlers;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import no.unit.nva.commons.json.JsonSerializable;
import no.unit.nva.identifiers.SortableIdentifier;

import nva.commons.core.JacocoGenerated;

import java.util.Objects;

public class DeleteResourceEvent implements JsonSerializable {

    public static final String EVENT_TOPIC = "PublicationService.ExpandedEntry.Deleted";

    private final String topic;
    private final SortableIdentifier identifier;

    @JsonCreator
    public DeleteResourceEvent(
            @JsonProperty("topic") String topic,
            @JsonProperty("identifier") SortableIdentifier identifier) {
        this.topic = topic;
        this.identifier = identifier;
    }

    @JacocoGenerated
    public String getTopic() {
        return topic;
    }

    public SortableIdentifier getIdentifier() {
        return identifier;
    }

    @JacocoGenerated
    @Override
    public int hashCode() {
        return Objects.hash(getTopic(), getIdentifier());
    }

    @Override
    @JacocoGenerated
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        DeleteResourceEvent that = (DeleteResourceEvent) o;
        return topic.equals(that.topic) && identifier.equals(that.identifier);
    }
}
