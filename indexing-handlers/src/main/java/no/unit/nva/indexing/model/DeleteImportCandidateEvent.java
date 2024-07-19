package no.unit.nva.indexing.model;

import no.unit.nva.commons.json.JsonSerializable;
import no.unit.nva.identifiers.SortableIdentifier;

public record DeleteImportCandidateEvent(
    String topic,
    SortableIdentifier identifier
) implements JsonSerializable {

    public static final String EVENT_TOPIC = "ImportCandidates.ExpandedEntry.Deleted";

}
