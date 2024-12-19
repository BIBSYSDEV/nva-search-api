package no.unit.nva.search.model.records;

import com.fasterxml.jackson.annotation.JsonInclude;

import nva.commons.core.paths.UriWrapper;

import java.util.List;

/**
 * UserSettings is a class that represents a user settings.
 *
 * @author Stig Norland
 */
@JsonInclude
public record UserSettings(List<String> promotedPublications) {
    @Override
    public List<String> promotedPublications() {
        return promotedPublications.stream()
                .map(id -> UriWrapper.fromUri(id).getLastPathElement())
                .toList();
    }
}
