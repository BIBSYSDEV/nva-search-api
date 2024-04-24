package no.unit.nva.search2.common;

import no.unit.nva.search2.common.security.CachedJwtProvider;
import org.opensearch.client.RequestOptions;


import static com.amazonaws.auth.internal.SignerConstants.AUTHORIZATION;

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

}
