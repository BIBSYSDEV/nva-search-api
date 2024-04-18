package no.unit.nva.search;

import static no.unit.nva.search.RestHighLevelClientWrapper.defaultRestHighLevelClientWrapper;
import static no.unit.nva.search.models.SearchResponseDto.createIdWithQuery;
import static no.unit.nva.search.models.SearchResponseDto.fromSearchResponse;
import java.io.IOException;
import java.net.URI;
import no.unit.nva.search.models.SearchDocumentsQuery;
import no.unit.nva.search.models.SearchResponseDto;
import nva.commons.apigateway.exceptions.ApiGatewayException;
import nva.commons.apigateway.exceptions.BadGatewayException;
import nva.commons.core.JacocoGenerated;
import nva.commons.secrets.SecretsReader;
import org.opensearch.action.search.SearchRequest;

public class SearchClient extends AuthenticatedOpenSearchClientWrapper {

    public static final String NO_RESPONSE_FROM_INDEX = "No response from index";
    public static final String ORGANIZATION_FIELD = "organization";
    public static final String ID_FIELD = "id";
    public static final String PART_OF_FIELD = "partOf";
    public static final String DOCUMENT_TYPE = "type";
    public static final String DOI_REQUEST = "DoiRequest";
    public static final String TICKET_STATUS = "status";

    /**
     * Creates a new SearchClient.
     *
     * @param openSearchClient client to use for access to the external search infrastructure
     * @param cachedJwt        A jwtProvider that will provide tokens
     */
    public SearchClient(RestHighLevelClientWrapper openSearchClient, CachedJwtProvider cachedJwt) {
        super(openSearchClient, cachedJwt);
    }

    @JacocoGenerated
    public static SearchClient defaultSearchClient() {
        return prepareWithSecretReader(new SecretsReader());
    }

    public static SearchClient prepareWithSecretReader(SecretsReader secretReader) {
        var cognitoCredentials = createCognitoCredentials(secretReader);
        var cognitoAuthenticator = CognitoAuthenticator.prepareWithCognitoCredentials(cognitoCredentials);
        var cachedJwtProvider = CachedJwtProvider.prepareWithAuthenticator(cognitoAuthenticator);
        return new SearchClient(defaultRestHighLevelClientWrapper(), cachedJwtProvider);
    }

    /**
     * Searches for a searchTerm or index:searchTerm in opensearch index.
     *
     * @param query query object
     * @throws ApiGatewayException thrown when uri is misconfigured, service i not available or interrupted
     */
    public SearchResponseDto searchWithSearchDocumentQuery(SearchDocumentsQuery query, String index)
        throws ApiGatewayException {
        try {
            SearchRequest searchRequest = query.toSearchRequest(index);
            return doSearch(searchRequest, query.getRequestUri(), query.getSearchTerm());
        } catch (IOException e) {
            throw new BadGatewayException(NO_RESPONSE_FROM_INDEX);
        }
    }

    private SearchResponseDto doSearch(SearchRequest searchRequest, URI requestUri, String searchTerm)
        throws IOException {
        var searchResponse = openSearchClient.search(searchRequest, getRequestOptions());
        var id = createIdWithQuery(requestUri, searchTerm);
        return fromSearchResponse(searchResponse, id);
    }
}
