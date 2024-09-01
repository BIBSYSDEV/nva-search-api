package no.unit.nva.indexingclient.models;

import static com.amazonaws.auth.internal.SignerConstants.AUTHORIZATION;

import static no.unit.nva.indexingclient.models.RestHighLevelClientWrapper.SEARCH_INFRASTRUCTURE_CREDENTIALS;

import no.unit.nva.auth.CognitoCredentials;
import no.unit.nva.indexingclient.constants.ApplicationConstants;
import no.unit.nva.search.common.jwt.CachedJwtProvider;
import no.unit.nva.search.common.records.UsernamePasswordWrapper;

import nva.commons.secrets.SecretsReader;

import org.opensearch.client.RequestOptions;

import java.net.URI;

public class AuthenticatedOpenSearchClientWrapper {

    protected final RestHighLevelClientWrapper openSearchClient;
    protected final CachedJwtProvider cachedJwtProvider;

    /**
     * Creates a new OpensearchClient.
     *
     * @param openSearchClient client to use for access to external search infrastructure
     */
    public AuthenticatedOpenSearchClientWrapper(
            RestHighLevelClientWrapper openSearchClient, CachedJwtProvider cachedJwtProvider) {
        this.openSearchClient = openSearchClient;
        this.cachedJwtProvider = cachedJwtProvider;
    }

    protected RequestOptions getRequestOptions() {
        var token = "Bearer " + cachedJwtProvider.getValue().getToken();
        return RequestOptions.DEFAULT.toBuilder().addHeader(AUTHORIZATION, token).build();
    }

    protected static CognitoCredentials createCognitoCredentials(SecretsReader secretsReader) {
        var credentials =
                secretsReader.fetchClassSecret(
                        SEARCH_INFRASTRUCTURE_CREDENTIALS, UsernamePasswordWrapper.class);
        var uri = URI.create(ApplicationConstants.SEARCH_INFRASTRUCTURE_AUTH_URI);

        return new CognitoCredentials(credentials::getUsername, credentials::getPassword, uri);
    }
}
