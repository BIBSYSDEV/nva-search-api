package no.unit.nva.search2.model;

import static no.unit.nva.search2.constant.ApplicationConstants.SEARCH_INFRASTRUCTURE_CREDENTIALS;
import static no.unit.nva.search2.constant.ApplicationConstants.readSearchInfrastructureAuthUri;
import java.net.URI;
import java.util.stream.Stream;
import no.unit.nva.auth.CognitoCredentials;
import no.unit.nva.search.CachedJwtProvider;
import no.unit.nva.search.CognitoAuthenticator;
import no.unit.nva.search.models.UsernamePasswordWrapper;
import nva.commons.core.JacocoGenerated;
import nva.commons.secrets.SecretsReader;
import org.jetbrains.annotations.NotNull;

public interface OpenSearchClient<R,Q extends OpenSearchQuery<?>> {

    R doSearch(Q query, String mediaType);

    @NotNull
    @JacocoGenerated
    static Stream<UsernamePasswordWrapper> getUsernamePasswordStream(SecretsReader secretsReader) {
        return Stream.of(
            secretsReader.fetchClassSecret(SEARCH_INFRASTRUCTURE_CREDENTIALS, UsernamePasswordWrapper.class));
    }

    @NotNull
    @JacocoGenerated
    static CognitoCredentials getCognitoCredentials(UsernamePasswordWrapper wrapper) {
        var uri = URI.create(readSearchInfrastructureAuthUri());
        return new CognitoCredentials(wrapper::getUsername, wrapper::getPassword, uri);
    }

    @NotNull
    @JacocoGenerated
    static CachedJwtProvider getCachedJwtProvider(SecretsReader reader) {
        return
            getUsernamePasswordStream(reader)
                .map(OpenSearchClient::getCognitoCredentials)
                .map(CognitoAuthenticator::prepareWithCognitoCredentials)
                .map(CachedJwtProvider::prepareWithAuthenticator)
                .findFirst().orElseThrow();
    }
}
