package no.unit.nva.search.common.records;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.util.List;

import static nva.commons.core.paths.UriWrapper.fromUri;


@JsonInclude
public record UserSettings(
    List<String> promotedPublications
) {
    @Override
    public List<String> promotedPublications() {
        return promotedPublications
            .stream().map(id -> fromUri(id).getLastPathElement())
            .toList();
    }
}
