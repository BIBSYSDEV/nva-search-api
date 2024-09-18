package no.unit.nva.search.common.records;

import static nva.commons.core.paths.UriWrapper.fromUri;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * @author Stig Norland
 */
@JsonInclude
public record UserSettings(List<String> promotedPublications) {
    public static CompletableFuture<UserSettings> empty() {
        return CompletableFuture.completedFuture(new UserSettings(List.of("")));
    }

    @Override
    public List<String> promotedPublications() {
        return promotedPublications.stream().map(id -> fromUri(id).getLastPathElement()).toList();
    }
}
