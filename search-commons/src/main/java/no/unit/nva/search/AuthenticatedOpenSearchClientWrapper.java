package no.unit.nva.search;

import static com.amazonaws.auth.internal.SignerConstants.AUTHORIZATION;
import static no.unit.nva.search.RestHighLevelClientWrapper.SEARCH_INFRASTRUCTURE_CREDENTIALS;
import static no.unit.nva.search.constants.ApplicationConstants.SEARCH_INFRASTRUCTURE_AUTH_URI;
import java.net.URI;
import no.unit.nva.auth.CognitoCredentials;
import no.unit.nva.search.models.UsernamePasswordWrapper;
import nva.commons.secrets.SecretsReader;
import org.opensearch.client.RequestOptions;

public class AuthenticatedOpenSearchClientWrapper {

    protected final RestHighLevelClientWrapper openSearchClient;
    protected final CognitoAuthenticator authenticator;

    /**
     * Creates a new OpensearchClient.
     *
     * @param openSearchClient client to use for access to ElasticSearch
     */
    public AuthenticatedOpenSearchClientWrapper(RestHighLevelClientWrapper openSearchClient,
                                                CognitoAuthenticator authenticator) {
        this.openSearchClient = openSearchClient;
        this.authenticator = authenticator;
    }


    protected RequestOptions getRequestOptions() {
        var token = "Bearer " + authenticator.getBearerToken().getToken();
        return RequestOptions.DEFAULT
                   .toBuilder()
                   .addHeader(AUTHORIZATION, token)
                   .build();
    }

    protected static CognitoCredentials createCognitoCredentials(SecretsReader secretsReader) {
        var credentials
            = secretsReader.fetchClassSecret(SEARCH_INFRASTRUCTURE_CREDENTIALS, UsernamePasswordWrapper.class);
        var uri = URI.create(SEARCH_INFRASTRUCTURE_AUTH_URI);

        return new CognitoCredentials(credentials::getUsername, credentials::getPassword, uri);
    }
}
