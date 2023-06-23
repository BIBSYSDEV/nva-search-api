package no.unit.nva.search;

import static no.unit.nva.search.RestHighLevelClientWrapper.defaultRestHighLevelClientWrapper;
import static no.unit.nva.search.models.SearchResponseDto.createIdWithQuery;
import static no.unit.nva.search.models.SearchResponseDto.fromSearchResponse;
import com.fasterxml.jackson.databind.JsonNode;
import com.opencsv.CSVWriter;
import java.io.IOException;
import java.io.StringWriter;
import java.net.URI;
import java.util.ArrayList;
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
        var cognitoAuthenticator
            = CognitoAuthenticator.prepareWithCognitoCredentials(cognitoCredentials);
        var cachedJwtProvider = CachedJwtProvider.prepareWithAuthenticator(cognitoAuthenticator);
        return new SearchClient(defaultRestHighLevelClientWrapper(), cachedJwtProvider);
    }

    public static String exportSearchResults(SearchResponseDto searchResponseDto) throws IOException {

        List<String[]> textData = createTextDataFromSearchResult(searchResponseDto.getHits());
        StringWriter writer = new StringWriter();
        CSVWriter csvwriter = new CSVWriter(writer, CSVWriter.DEFAULT_SEPARATOR, CSVWriter.NO_QUOTE_CHARACTER,
                                            CSVWriter.NO_ESCAPE_CHARACTER, CSVWriter.DEFAULT_LINE_END);
        csvwriter.writeAll(textData);
        csvwriter.close();

        return writer.toString();
    }

    public static List<String[]> createTextDataFromSearchResult(List<JsonNode> searchResults) {

        String[] header = {"id", "mainTitle", "publicationYear", "publicationMonth", "publicationDay",
            "publicationInstance", "contributorNames"};
        List<String[]> createTextData = new ArrayList<>();
        createTextData.add(header);

        List<String> contributors = new ArrayList<>();
        for (JsonNode jsonNode : searchResults) {
            var entityDescription = jsonNode.get("entityDescription");
            for (JsonNode jsonNode2 : entityDescription.get("contributors")) {
                contributors.add(String.valueOf(jsonNode2.get("identity").get("name")).replace("\"", ""));
            }

            String contributorsNames = '"' + String.join(",", contributors) + '"';

            String[] textData = {
                jsonNode.get("id").toString(),
                entityDescription.get("mainTitle").toString(),
                entityDescription.get("publicationDate").get("year").toString(),
                entityDescription.get("publicationDate").get("month").toString(),
                entityDescription.get("publicationDate").get("day").toString(),
                entityDescription.get("reference").get("publicationInstance").get("type").toString(),
                contributorsNames};

            createTextData.add(textData);
            contributors.removeAll(contributors);
        }

        return createTextData;
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
        try {
            SearchRequest searchRequest = query.toSearchRequest(index);
            return doSearch(searchRequest, query.getRequestUri(), query.getSearchTerm());
        } catch (IOException e) {
            throw new BadGatewayException(NO_RESPONSE_FROM_INDEX);
        }
    }

    public SearchResponseDto searchWithSearchTicketQuery(
        ViewingScope viewingScope,
        SearchTicketsQuery searchTicketsQuery,
        String... index
    ) throws ApiGatewayException {
        try {
            SearchRequest searchRequest = searchTicketsQuery.createSearchRequestForTicketsWithOrganizationIds(
                viewingScope,
                index);
            return doSearch(searchRequest, searchTicketsQuery.getRequestUri(), searchTicketsQuery.getSearchTerm());
        } catch (IOException e) {
            throw new BadGatewayException(NO_RESPONSE_FROM_INDEX);
        }
    }

    public SearchResponseDto searchOwnerTickets(
        SearchTicketsQuery searchTicketsQuery,
        String owner,
        String... index) throws BadGatewayException {
        try {
            var searchRequest = searchTicketsQuery.createSearchRequestForTicketsByOwner(owner, index);
            return doSearch(searchRequest, searchTicketsQuery.getRequestUri(), searchTicketsQuery.getSearchTerm());
        } catch (IOException e) {
            throw new BadGatewayException(NO_RESPONSE_FROM_INDEX);
        }
    }

    public String exportSearchWithDocumentQuery(SearchDocumentsQuery query,
                                                String index) throws ApiGatewayException, IOException {
        var searchResponseDto = searchWithSearchDocumentQuery(query, index);
        return exportSearchResults(searchResponseDto);
    }

    private SearchResponseDto doSearch(SearchRequest searchRequest, URI requestUri, String searchTerm)
        throws IOException {
        var searchResponse = openSearchClient.search(searchRequest, getRequestOptions());
        var id = createIdWithQuery(requestUri, searchTerm);
        return fromSearchResponse(searchResponse, id);
    }
}
