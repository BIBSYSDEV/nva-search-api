package no.unit.nva.indexing.client.keybatch;

import no.unit.nva.commons.json.JsonSerializable;
import no.unit.nva.events.models.EventBody;

public record KeyBatchRequestEvent(String startMarker, String topic, String location)
        implements JsonSerializable, EventBody {

    @Override
    public String getTopic() {
        return topic;
    }
}
