package no.unit.nva.search2;

import no.unit.nva.search.*;
import no.unit.nva.search.models.SearchResponseDto;
import nva.commons.core.JacocoGenerated;
import nva.commons.secrets.SecretsReader;
import org.opensearch.action.search.SearchRequest;

import java.io.IOException;
import java.net.URI;

import static no.unit.nva.search.RestHighLevelClientWrapper.defaultRestHighLevelClientWrapper;
import static no.unit.nva.search.models.SearchResponseDto.fromSearchResponse;

public class SwsQueryClient  extends AuthenticatedOpenSearchClientWrapper {
    /**
     * Creates a new OpensearchClient.
     *
     * @param openSearchClient  client to use for access to external search infrastructure
     * @param cachedJwtProvider
     */
    public SwsQueryClient(RestHighLevelClientWrapper openSearchClient,
                          CachedJwtProvider cachedJwtProvider) {
        super(openSearchClient, cachedJwtProvider);
    }


    @JacocoGenerated
    public static SwsQueryClient defaultSwsClient() {
        return prepareWithSecretReader(new SecretsReader());
    }

    public static SwsQueryClient prepareWithSecretReader(SecretsReader secretReader) {
        var cognitoCredentials = createCognitoCredentials(secretReader);
        var cognitoAuthenticator = CognitoAuthenticator.prepareWithCognitoCredentials(cognitoCredentials);
        var cachedJwtProvider = CachedJwtProvider.prepareWithAuthenticator(cognitoAuthenticator);
        return new SwsQueryClient(defaultRestHighLevelClientWrapper(), cachedJwtProvider);
    }

    protected SearchResponseDto doSearch(SearchRequest searchRequest, URI requestUri)
        throws IOException {
        var searchResponse = openSearchClient.search(searchRequest, getRequestOptions());
        return fromSearchResponse(searchResponse, requestUri);
    }
}
