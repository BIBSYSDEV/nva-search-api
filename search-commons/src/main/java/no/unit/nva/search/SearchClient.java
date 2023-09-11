package no.unit.nva.search;

import static no.unit.nva.search.RestHighLevelClientWrapper.defaultRestHighLevelClientWrapper;
import static no.unit.nva.search.models.SearchResponseDto.createIdWithQuery;
import static no.unit.nva.search.models.SearchResponseDto.fromSearchResponse;
import java.io.IOException;
import java.net.URI;
import java.util.List;
import no.unit.nva.search.models.SearchDocumentsQuery;
import no.unit.nva.search.models.SearchResponseDto;
import no.unit.nva.search.models.SearchTicketsQuery;
import no.unit.nva.search.restclients.responses.ViewingScope;
import nva.commons.apigateway.exceptions.ApiGatewayException;
import nva.commons.apigateway.exceptions.BadGatewayException;
import nva.commons.core.JacocoGenerated;
import nva.commons.secrets.SecretsReader;
import org.opensearch.action.search.SearchRequest;
import org.opensearch.index.query.BoolQueryBuilder;
import org.opensearch.index.query.QueryBuilders;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
    public static final String CONTRIBUTOR_ID_FIELD = "entityDescription.contributors.identity.id";

    private static final Logger logger = LoggerFactory.getLogger(SearchClient.class);

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

    public SearchResponseDto searchWithSearchPromotedPublicationsForContributorQuery(String owner,
                                                                                     List<String> promotedPublications,
                                                                                     SearchDocumentsQuery query,
                                                                                     String index)
        throws ApiGatewayException {
        try {
            var queryBuilder = new BoolQueryBuilder().must(QueryBuilders.matchQuery(CONTRIBUTOR_ID_FIELD, owner));
            for (int i = 0; i < promotedPublications.size(); i++) {
                queryBuilder.should(
                    QueryBuilders.matchQuery("id", promotedPublications.get(i)).boost(promotedPublications.size() - i));
            }

            var searchRequest = query.toSearchRequestWithBoolQuery(index, queryBuilder);

            return doSearch(searchRequest, query.getRequestUri(), query.getSearchTerm());
        } catch (IOException e) {
            throw new BadGatewayException(NO_RESPONSE_FROM_INDEX);
        }
    }

    public SearchResponseDto searchWithSearchTicketQuery(ViewingScope viewingScope,
                                                         SearchTicketsQuery searchTicketsQuery, String... index)
        throws ApiGatewayException {
        try {
            SearchRequest searchRequest = searchTicketsQuery.createSearchRequestForTicketsWithOrganizationIds(
                viewingScope, index);
            return doSearch(searchRequest, searchTicketsQuery.getRequestUri(), searchTicketsQuery.getSearchTerm());
        } catch (IOException e) {
            throw new BadGatewayException(NO_RESPONSE_FROM_INDEX);
        }
    }

    public SearchResponseDto searchOwnerTickets(SearchTicketsQuery searchTicketsQuery, String owner, String... index)
        throws BadGatewayException {
        try {
            var searchRequest = searchTicketsQuery.createSearchRequestForTicketsByOwner(owner, index);
            return doSearch(searchRequest, searchTicketsQuery.getRequestUri(), searchTicketsQuery.getSearchTerm());
        } catch (IOException e) {
            throw new BadGatewayException(NO_RESPONSE_FROM_INDEX);
        }
    }

    public String exportSearchWithDocumentQuery(SearchDocumentsQuery query, String index)
        throws ApiGatewayException, IOException {
        var searchResponseDto = searchWithSearchDocumentQuery(query, index);
        return CsvTransformer.transform(searchResponseDto);
    }

    private SearchResponseDto doSearch(SearchRequest searchRequest, URI requestUri, String searchTerm)
        throws IOException {
        logger.info("SearchRequest: {}", searchRequest.toString());
        var searchResponse = openSearchClient.search(searchRequest, getRequestOptions());
        var id = createIdWithQuery(requestUri, searchTerm);
        logger.info("request uri: {}", id );
        return fromSearchResponse(searchResponse, id);
    }
}
