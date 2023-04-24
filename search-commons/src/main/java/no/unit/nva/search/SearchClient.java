package no.unit.nva.search;

import no.unit.nva.search.models.SearchDocumentsQuery;
import no.unit.nva.search.models.SearchResponseDto;
import no.unit.nva.search.models.SearchTicketsQuery;
import no.unit.nva.search.restclients.responses.ViewingScope;
import nva.commons.apigateway.exceptions.ApiGatewayException;
import nva.commons.apigateway.exceptions.BadGatewayException;
import nva.commons.core.JacocoGenerated;
import nva.commons.secrets.SecretsReader;
import org.opensearch.action.search.SearchRequest;
import org.opensearch.action.search.SearchResponse;

import java.io.IOException;

import static no.unit.nva.search.RestHighLevelClientWrapper.defaultRestHighLevelClientWrapper;
import static no.unit.nva.search.models.SearchResponseDto.createIdWithQuery;
import static no.unit.nva.search.models.SearchResponseDto.fromSearchResponse;

public class SearchClient extends AuthenticatedOpenSearchClientWrapper {

    public static final String NO_RESPONSE_FROM_INDEX = "No response from index";
    public static final String ORGANIZATION_IDS = "organizationIds";
    public static final String PUBLICATION_STATUS = "publication.status";
    public static final String DRAFT_PUBLICATION_STATUS = "DRAFT";
    public static final String DOCUMENT_TYPE = "type";
    public static final String DOI_REQUEST = "DoiRequest";
    public static final String GENERAL_SUPPORT_CASE = "GeneralSupportCase";
    public static final String PUBLISHING_REQUEST = "PublishingRequest";
    public static final String GENERAL_SUPPORT_QUERY_NAME = "GeneralSupportQuery";
    public static final String DOI_REQUESTS_QUERY_NAME = "DoiRequestsQuery";
    public static final String PUBLISHING_REQUESTS_QUERY_NAME = "PublishingRequestsQuery";
    public static final String INCLUDED_VIEWING_SCOPES_QUERY_NAME = "IncludedViewingScopesQuery";
    public static final String EXCLUDED_VIEWING_SCOPES_QUERY_NAME = "ExcludedViewingScopesQuery";
    public static final String TICKET_STATUS = "status";
    public static final String PENDING = "Pending";

    /**
     * Creates a new SearchClient.
     *
     * @param openSearchClient client to use for access to the external search infrastructure
     * @param cachedJwt        A jwtProvider that will provide tokens
     */
    public SearchClient(RestHighLevelClientWrapper openSearchClient, CachedJwtProvider cachedJwt) {
        super(openSearchClient, cachedJwt);
    }

    /**
     * Searches for a searchTerm or index:searchTerm in opensearch index.
     *
     * @param query query object
     * @throws ApiGatewayException thrown when uri is misconfigured, service i not available or interrupted
     */
    public SearchResponseDto searchWithSearchDocumentQuery(
            SearchDocumentsQuery query,
            String index
    ) throws ApiGatewayException {

        var searchResponse = doSearch(query, index);
        var id = createIdWithQuery(query.getRequestUri(), query.getSearchTerm());
        return fromSearchResponse(searchResponse, id);
    }

    public SearchResponseDto searchWithSearchTicketQuery(
            ViewingScope viewingScope,
            SearchTicketsQuery searchTicketsQuery,
            String... index
    ) throws ApiGatewayException {
        var searchResponse = findTicketsForOrganizationIds(viewingScope, searchTicketsQuery, index);
        var id = createIdWithQuery(searchTicketsQuery.getRequestUri(), searchTicketsQuery.getSearchTerm());
        return fromSearchResponse(searchResponse, id);
    }

    public SearchResponse findTicketsForOrganizationIds(ViewingScope viewingScope,
                                                        SearchTicketsQuery searchTicketsQuery,
                                                        String... index)
            throws BadGatewayException {
        try {
            SearchRequest searchRequest = searchTicketsQuery.createSearchRequestForTicketsWithOrganizationIds(
                    viewingScope,
                    index);
            return openSearchClient.search(searchRequest, getRequestOptions());
        } catch (IOException e) {
            throw new BadGatewayException(NO_RESPONSE_FROM_INDEX);
        }
    }

    public SearchResponseDto searchOwnerTickets(
            SearchTicketsQuery searchTicketsQuery,
            String owner,
            String... index) throws BadGatewayException {
        var searchResponse = findTicketsForCreator(searchTicketsQuery, owner, index);
        var id = createIdWithQuery(searchTicketsQuery.getRequestUri(), searchTicketsQuery.getSearchTerm());
        return fromSearchResponse(searchResponse, id);

    }


    public SearchResponse findTicketsForCreator(SearchTicketsQuery searchTicketsQuery, String owner, String... index)
            throws BadGatewayException {
        try {
            var searchRequest = searchTicketsQuery.createSearchRequestForTicketsByOwner(owner, index);
            return openSearchClient.search(searchRequest, getRequestOptions());
        } catch (IOException e) {
            throw new BadGatewayException(NO_RESPONSE_FROM_INDEX);
        }
    }

    public SearchResponse doSearch(SearchDocumentsQuery query, String index) throws BadGatewayException {
        try {
            SearchRequest searchRequest = query.toSearchRequest(index);
            return openSearchClient.search(searchRequest, getRequestOptions());
        } catch (IOException e) {
            throw new BadGatewayException(NO_RESPONSE_FROM_INDEX);
        }
    }

    @JacocoGenerated
    public static SearchClient defaultSearchClient() {
        return prepareWithSecretReader(new SecretsReader());
    }

    public static SearchClient prepareWithSecretReader(SecretsReader secretReader) {
        var cognitoCredentials = createCognitoCredentials(secretReader);
        var cognitoAuthenticator
                = CognitoAuthenticator.prepareWithCognitoCredentials(cognitoCredentials);
        var cachedJwtProvider = CachedJwtProvider.prepareWithAuthenticator(cognitoAuthenticator);
        return new SearchClient(defaultRestHighLevelClientWrapper(), cachedJwtProvider);
    }
}
