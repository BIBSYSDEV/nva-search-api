package no.unit.nva.indexingclient.keybatch;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.Objects;
import no.unit.nva.commons.json.JsonSerializable;
import no.unit.nva.events.models.EventBody;
import nva.commons.core.JacocoGenerated;

public class KeyBatchRequestEvent implements JsonSerializable, EventBody {

    public static final String START_MARKER = "startMarker";
    public static final String TOPIC = "topic";
    public static final String LOCATION = "location";
    @JsonProperty(START_MARKER)
    private final String startMarker;
    @JsonProperty(TOPIC)
    private final String topic;
    @JsonProperty(LOCATION)
    private final String location;

    @JsonCreator
    public KeyBatchRequestEvent(@JsonProperty(START_MARKER) String startMarker, @JsonProperty(TOPIC) String topic,
                                @JsonProperty(LOCATION) String location) {
        this.startMarker = startMarker;
        this.topic = topic;
        this.location = location;
    }

    public String getLocation() {
        return location;
    }

    @JacocoGenerated
    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        KeyBatchRequestEvent that = (KeyBatchRequestEvent) o;
        return Objects.equals(getStartMarker(), that.getStartMarker())
               && Objects.equals(getTopic(), that.getTopic())
               && Objects.equals(getLocation(), that.getLocation());
    }

    @JacocoGenerated
    @Override
    public int hashCode() {
        return Objects.hash(getStartMarker(), getTopic(), getLocation());
    }

    @Override
    public String getTopic() {
        return topic;
    }

    public String getStartMarker() {
        return startMarker;
    }
}
