package no.unit.nva.search2.common.records;

import no.unit.nva.commons.json.JsonSerializable;

import java.net.URI;
import java.util.List;

public record LogUserSettings(URI uri, List<String> promotedPublications) implements JsonSerializable {
    public LogUserSettings(URI uri, UserSettings userSettings) {
        this(uri, userSettings.promotedPublications());
    }
}
