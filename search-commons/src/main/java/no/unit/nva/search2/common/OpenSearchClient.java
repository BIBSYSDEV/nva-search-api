package no.unit.nva.search2.common;

import static no.unit.nva.search2.constant.Functions.readSearchInfrastructureAuthUri;
import static no.unit.nva.search2.constant.Words.SEARCH_INFRASTRUCTURE_CREDENTIALS;
import java.net.URI;
import java.util.stream.Stream;
import no.unit.nva.auth.CognitoCredentials;
import no.unit.nva.search.CachedJwtProvider;
import no.unit.nva.search.CognitoAuthenticator;
import no.unit.nva.search.models.UsernamePasswordWrapper;
import nva.commons.core.JacocoGenerated;
import nva.commons.secrets.SecretsReader;
import org.jetbrains.annotations.NotNull;

public abstract class OpenSearchClient<R, Q extends Query<?>> {

    public abstract R doSearch(Q query);

    @NotNull
    @JacocoGenerated
    public static Stream<UsernamePasswordWrapper> getUsernamePasswordStream(SecretsReader secretsReader) {
        return Stream.of(
            secretsReader.fetchClassSecret(SEARCH_INFRASTRUCTURE_CREDENTIALS, UsernamePasswordWrapper.class));
    }

    @NotNull
    @JacocoGenerated
    public static CognitoCredentials getCognitoCredentials(UsernamePasswordWrapper wrapper) {
        var uri = URI.create(readSearchInfrastructureAuthUri());
        return new CognitoCredentials(wrapper::getUsername, wrapper::getPassword, uri);
    }

    @NotNull
    @JacocoGenerated
    public static CachedJwtProvider getCachedJwtProvider(SecretsReader reader) {
        return
            getUsernamePasswordStream(reader)
                .map(OpenSearchClient::getCognitoCredentials)
                .map(CognitoAuthenticator::prepareWithCognitoCredentials)
                .map(CachedJwtProvider::prepareWithAuthenticator)
                .findFirst().orElseThrow();
    }
}
