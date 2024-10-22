package no.unit.nva.search.model.records;

import static no.unit.nva.search.model.constant.Defaults.ENVIRONMENT;
import static no.unit.nva.search.model.constant.Words.AUTHORIZATION;

import no.unit.nva.auth.CognitoCredentials;
import no.unit.nva.search.model.jwt.CachedJwtProvider;

import nva.commons.secrets.SecretsReader;

import org.opensearch.client.RequestOptions;

import java.net.URI;

public class AuthenticatedOpenSearchClientWrapper {

    static final String SEARCH_INFRASTRUCTURE_CREDENTIALS = "SearchInfrastructureCredentials";
    static final String SEARCH_INFRASTRUCTURE_AUTH_URI =
            ENVIRONMENT.readEnv("SEARCH_INFRASTRUCTURE_AUTH_URI");

    protected final RestHighLevelClientWrapper openSearchClient;
    protected final CachedJwtProvider cachedJwtProvider;

    /**
     * Creates a new OpensearchClient.
     *
     * @param openSearchClient client to use for access to external search infrastructure
     * @param cachedJwtProvider provider for JWT tokens
     */
    public AuthenticatedOpenSearchClientWrapper(
            RestHighLevelClientWrapper openSearchClient, CachedJwtProvider cachedJwtProvider) {
        this.openSearchClient = openSearchClient;
        this.cachedJwtProvider = cachedJwtProvider;
    }

    /**
     * Creates a new OpensearchClient.
     *
     * @param secretsReader reader for secrets (used to fetch the username and password for the
     *     search infrastructure)
     * @return CognitoCredentials for the search infrastructure
     */
    protected static CognitoCredentials createCognitoCredentials(SecretsReader secretsReader) {
        var credentials =
                secretsReader.fetchClassSecret(
                        SEARCH_INFRASTRUCTURE_CREDENTIALS, UsernamePasswordWrapper.class);
        var uri = URI.create(SEARCH_INFRASTRUCTURE_AUTH_URI);

        return new CognitoCredentials(credentials::getUsername, credentials::getPassword, uri);
    }

    protected RequestOptions getRequestOptions() {
        var token = "Bearer " + cachedJwtProvider.getValue().getToken();
        return RequestOptions.DEFAULT.toBuilder().addHeader(AUTHORIZATION, token).build();
    }
}
