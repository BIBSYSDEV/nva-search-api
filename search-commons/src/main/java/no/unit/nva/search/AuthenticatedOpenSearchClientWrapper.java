package no.unit.nva.search;

import static com.amazonaws.auth.internal.SignerConstants.AUTHORIZATION;
import static no.unit.nva.search.RestHighLevelClientWrapper.SEARCH_INFRASTRUCTURE_CREDENTIALS;
import static no.unit.nva.search.constants.ApplicationConstants.SEARCH_INFRASTRUCTURE_AUTH_URI;
import java.net.URI;
import no.unit.nva.auth.CognitoCredentials;
import no.unit.nva.search2.common.records.UsernamePasswordWrapper;
import no.unit.nva.search2.common.jwt.CachedJwtProvider;
import nva.commons.secrets.SecretsReader;
import org.opensearch.client.RequestOptions;

public class AuthenticatedOpenSearchClientWrapper {

    protected final RestHighLevelClientWrapper openSearchClient;
    protected final CachedJwtProvider cachedJwtProvider;

    /**
     * Creates a new OpensearchClient.
     *
     * @param openSearchClient client to use for access to external search infrastructure
     */
    public AuthenticatedOpenSearchClientWrapper(RestHighLevelClientWrapper openSearchClient,
                                                CachedJwtProvider cachedJwtProvider) {
        this.openSearchClient = openSearchClient;
        this.cachedJwtProvider = cachedJwtProvider;
    }


    protected RequestOptions getRequestOptions() {
        var token = "Bearer " + cachedJwtProvider.getValue().getToken();
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
