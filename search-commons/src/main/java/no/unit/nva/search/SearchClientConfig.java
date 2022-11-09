package no.unit.nva.search;

import no.unit.nva.auth.CognitoCredentials;
import no.unit.nva.search.models.UsernamePasswordWrapper;
import nva.commons.core.JacocoGenerated;
import nva.commons.secrets.SecretsReader;

import java.net.URI;

import static no.unit.nva.search.RestHighLevelClientWrapper.*;
import static no.unit.nva.search.constants.ApplicationConstants.SEARCH_INFRASTRUCTURE_AUTH_URI;

public final class SearchClientConfig {

    private SearchClientConfig() {

    }

    @JacocoGenerated
    public static SearchClient defaultSearchClient() {
        return prepareWithSecretReader(new SecretsReader());
    }

    public static SearchClient prepareWithSecretReader(SecretsReader secretReader) {
        var cognitoCredentials = createCognitoCredentials(secretReader);
        var cognitoAuthenticator
                = CognitoAuthenticator.prepareWithCognitoCredentials(cognitoCredentials);
        return new SearchClient(defaultRestHighLevelClientWrapper(), cognitoAuthenticator);
    }

    private static CognitoCredentials createCognitoCredentials(SecretsReader secretsReader) {
        var credentials
                = secretsReader.fetchClassSecret(SEARCH_INFRASTRUCTURE_CREDENTIALS, UsernamePasswordWrapper.class);
        var uri = URI.create(SEARCH_INFRASTRUCTURE_AUTH_URI);

        return new CognitoCredentials(credentials::getUsername, credentials::getPassword, uri);
    }

}
