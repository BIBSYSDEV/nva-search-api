package no.unit.nva.search;

import static java.net.HttpURLConnection.HTTP_FORBIDDEN;
import static java.net.HttpURLConnection.HTTP_OK;
import static no.unit.nva.indexing.testutils.MockedJwtProvider.setupMockedCachedJwtProvider;
import static no.unit.nva.search.RequestUtil.DOMAIN_NAME;
import static no.unit.nva.search.RequestUtil.PATH;
import static no.unit.nva.search.RequestUtil.SEARCH_TERM_KEY;
import static no.unit.nva.search.RequestUtil.VIEWING_SCOPE_KEY;
import static no.unit.nva.search.SearchTicketsHandler.ACCESS_RIGHTS_TO_VIEW_TICKETS;
import static no.unit.nva.search.SearchTicketsHandler.ROLE_CREATOR;
import static no.unit.nva.search.constants.ApplicationConstants.objectMapperWithEmpty;
import static no.unit.nva.testutils.RandomDataGenerator.randomInteger;
import static no.unit.nva.testutils.RandomDataGenerator.randomString;
import static no.unit.nva.testutils.RandomDataGenerator.randomUri;
import static nva.commons.core.ioutils.IoUtils.stringFromResources;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import com.amazonaws.services.lambda.runtime.Context;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.google.common.net.HttpHeaders;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import no.unit.nva.auth.uriretriever.AuthorizedBackendUriRetriever;
import no.unit.nva.indexing.testutils.SearchResponseUtil;
import no.unit.nva.search.models.SearchResponseDto;
import no.unit.nva.testutils.HandlerRequestBuilder;
import nva.commons.apigateway.AccessRight;
import nva.commons.apigateway.GatewayResponse;
import nva.commons.core.Environment;
import nva.commons.core.ioutils.IoUtils;
import org.hamcrest.CoreMatchers;
import org.hamcrest.core.Is;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.opensearch.action.search.SearchResponse;
import org.opensearch.client.RestHighLevelClient;
import org.zalando.problem.Problem;

class SearchTicketsHandlerTest {

    public static final String SAMPLE_OPENSEARCH_TICKETS_RESPONSE_JSON = "sample_opensearch_tickets_response.json";
    public static final String ROUNDTRIP_RESPONSE_TICKETS_JSON = "roundtrip_tickets_response.json";
    public static final String TICKET_ID = "0185dede1522-f1cff045-bf63-47d4-8be6-71817d409c8d";
    public static final URI CUSTOMER_CRISTIN_ID = URI.create("https://example.org/123.XXX.XXX.XXX");
    public static final URI SOME_LEGAL_CUSTOM_CRISTIN_ID = URI.create("https://example.org/123.111.222.333");
    public static final URI SOME_ILLEGAL_CUSTOM_CRISTIN_ID = URI.create("https://example.org/124.111.222.333");
    public static final String MESSAGES_PATH = "/messages";
    public static final String SAMPLE_DOMAIN_NAME = "localhost";
    public static final String ROLE = "role";
    public static final String COMPLETED = "Completed";
    public static final String SAMPLE_PATH = "search";
    public static final String SAMPLE_SEARCH_TERM = "searchTerm";
    private static final String USERNAME = randomString();
    private static final String ORGANIZATION_IDS = "https://www.example.com/20754.0.0.0";
    public static final URI TOP_LEVEL_CRISTIN_ORG_ID = URI.create(
        "https://api.dev.nva.aws.unit.no/cristin/organization/20754.0.0.0");
    public static final String COMMA = ",";
    private SearchTicketsHandler handler;
    private Context context;
    private ByteArrayOutputStream outputStream;
    private FakeRestHighLevelClientWrapper restHighLevelClientWrapper;
    private AuthorizedBackendUriRetriever uriRetriever;

    @BeforeEach
    void init() throws IOException {
        var cachedJwtProvider = setupMockedCachedJwtProvider();
        prepareSearchClientWithResponse();
        var searchClient = new SearchClient(restHighLevelClientWrapper, cachedJwtProvider);
        setupFakeUriRetriever();
        handler = new SearchTicketsHandler(new Environment(), searchClient, uriRetriever);
        context = mock(Context.class);
        outputStream = new ByteArrayOutputStream();
    }

    @Test
    void shouldReturnSearchResponseWithSearchHit() throws IOException {
        var inputStream = queryWithoutQueryParameters();
        handler.handleRequest(inputStream, outputStream, context);

        var response = GatewayResponse.fromOutputStream(outputStream, String.class);

        assertThat(response.getStatusCode(), is(equalTo(HTTP_OK)));
        assertThat(response.getBody(), containsString(TICKET_ID));

        JsonNode jsonNode = objectMapperWithEmpty.readTree(response.getBody());
        assertThat(jsonNode, is(notNullValue()));
    }

    @Test
    void shouldReturnSearchResultsForCreator() throws IOException {
        var inputStream = queryWithRole(ROLE_CREATOR);
        handler.handleRequest(inputStream, outputStream, context);

        var response = GatewayResponse.fromOutputStream(outputStream, String.class);

        assertThat(response.getStatusCode(), is(equalTo(HTTP_OK)));
        assertThat(response.getBody(), containsString(TICKET_ID));

        JsonNode jsonNode = objectMapperWithEmpty.readTree(response.getBody());
        assertThat(jsonNode, is(notNullValue()));
    }

    @Test
    void shouldReturnOkWhenSearchingWithDefaultViewingScopeWhenNoViewingScopeIsQueried()
        throws IOException {
        var inputStream = getInputStream();
        handler.handleRequest(inputStream, outputStream, context);
        var response = GatewayResponse.fromOutputStream(outputStream, String.class);
        assertThat(response.getStatusCode(), is(equalTo(HTTP_OK)));
    }

    @Test
    void shouldReturnForbiddenWhenSearchingWithViewingScopeOutsideOfTopLevelOrg() throws IOException {
        var inputStream = queryWithCustomOrganizationAsQueryParameter(List.of(randomUri()));
        handler.handleRequest(inputStream, outputStream, context);
        var response = GatewayResponse.fromOutputStream(outputStream, String.class);
        assertThat(response.getStatusCode(), is(equalTo(HTTP_FORBIDDEN)));
    }

    @Test
    void shouldReturnOkWhenSearchingWithViewingScopeThatIsPartOfTopLevelCristinOrg()
        throws URISyntaxException, IOException {
        var uri = new URI("https://api.dev.nva.aws.unit.no/cristin/organization/20754.4.0.0");
        var inputStream = queryWithCustomOrganizationAsQueryParameter(List.of(uri));
        handler.handleRequest(inputStream, outputStream, context);
        var response = GatewayResponse.fromOutputStream(outputStream, String.class);
        assertThat(response.getStatusCode(), is(equalTo(HTTP_OK)));
    }

    @Test
    void shouldNotSendQueryAndReturnForbiddenWhenUserRequestsToViewDoiRequestsOrMessagesOutsideTheirLegalScope()
        throws IOException {
        handler.handleRequest(queryWithCustomOrganizationAsQueryParameter(List.of(SOME_ILLEGAL_CUSTOM_CRISTIN_ID)),
                              outputStream,
                              context);
        var response = GatewayResponse.fromOutputStream(outputStream, Problem.class);
        assertThat(response.getStatusCode(), is(equalTo(HttpURLConnection.HTTP_FORBIDDEN)));

        var searchRequest = restHighLevelClientWrapper.getSearchRequest();
        assertThat(searchRequest, is(nullValue()));
    }

    @Test
    void shouldNotSendQueryAndReturnForbiddenWhenUserDoesNotHaveTheAppropriateAccessRigth() throws IOException {
        handler.handleRequest(queryWithoutAppropriateAccessRight(), outputStream, context);
        var response = GatewayResponse.fromOutputStream(outputStream, Problem.class);
        assertThat(response.getStatusCode(), is(equalTo(HttpURLConnection.HTTP_FORBIDDEN)));
        var searchRequest = restHighLevelClientWrapper.getSearchRequest();
        assertThat(searchRequest, is(nullValue()));
    }

    @ParameterizedTest(name = "should send request to index specified in path")
    @ValueSource(strings = {"/messages", "/doirequests"})
    void shouldReturnIndexNameFromPath(String path) throws IOException {
        handler.handleRequest(queryWithoutQueryParameters(path), outputStream, context);
        var searchRequest = restHighLevelClientWrapper.getSearchRequest();
        var indices = Arrays.stream(searchRequest.indices()).collect(Collectors.toList());
        assertThat(indices, contains(path.substring(1)));
    }

    @Test
    void shouldSendSearchQueryWithPagination() throws IOException {
        var requestedPageNumber = 1 + randomInteger(20);
        var requestedPageSize = 2 + randomInteger(100);
        var request = queryWithPaginationParameters("tickets", requestedPageNumber, requestedPageSize);
        handler.handleRequest(request, outputStream, context);
        var searchRequest = restHighLevelClientWrapper.getSearchRequest();
        assertThat(searchRequest.source().from(), is(equalTo(requestedPageNumber)));
        assertThat(searchRequest.source().size(), is(equalTo(requestedPageSize)));
    }

    @Test
    void shouldReturnAggregationAsPartOfResponseWhenDoingASearch() throws IOException {
        handler.handleRequest(getInputStream(), outputStream, context);
        var gatewayResponse = GatewayResponse
                                  .fromOutputStream(outputStream, SearchResponseDto.class);

        var actual = gatewayResponse.getBodyObject(SearchResponseDto.class);
        var expected = objectMapperWithEmpty
                           .readValue(stringFromResources(Path.of(ROUNDTRIP_RESPONSE_TICKETS_JSON)),
                                      SearchResponseDto.class);

        assertNotNull(gatewayResponse.getHeaders());
        assertEquals(HTTP_OK, gatewayResponse.getStatusCode());
        assertThat(actual, is(equalTo(expected)));
        assertNotNull(actual.getAggregations());
        assertNotNull(actual.getAggregations().get("Bidragsyter"));
    }

    @Test
    void shouldReturnSearchResponseWithOrganizationIdsAndStatusWhenSearchingForTickets() throws IOException {
        handler.handleRequest(queryWithoutQueryParameters(), outputStream, context);

        var response = GatewayResponse.fromOutputStream(outputStream, String.class);

        assertThat(response.getStatusCode(), is(equalTo(HTTP_OK)));
        assertThat(response.getBody(), containsString(ORGANIZATION_IDS));
        assertThat(response.getBody(), containsString(COMPLETED));

        JsonNode jsonNode = objectMapperWithEmpty.readTree(response.getBody());
        assertThat(jsonNode, is(notNullValue()));
    }

    @Test
    void shouldReturnSearchResultsWhenQueryIsSingleTerm() throws IOException {
        handler.handleRequest(getInputStream(), outputStream, mock(Context.class));

        var gatewayResponse = GatewayResponse
                                  .fromOutputStream(outputStream, SearchResponseDto.class);
        var actual = gatewayResponse.getBodyObject(SearchResponseDto.class);

        var expected = objectMapperWithEmpty
                           .readValue(stringFromResources(Path.of(ROUNDTRIP_RESPONSE_TICKETS_JSON)),
                                      SearchResponseDto.class);

        assertNotNull(gatewayResponse.getHeaders());
        assertEquals(HTTP_OK, gatewayResponse.getStatusCode());
        assertThat(actual, Is.is(CoreMatchers.equalTo(expected)));
    }

    private InputStream queryWithPaginationParameters(String path, Integer from, Integer resultSize)
        throws JsonProcessingException {
        var customerId = randomUri();
        return new HandlerRequestBuilder<Void>(objectMapperWithEmpty)
                   .withUserName(USERNAME)
                   .withHeaders(defaultQueryHeaders())
                   .withCurrentCustomer(customerId)
                   .withAccessRights(customerId, ACCESS_RIGHTS_TO_VIEW_TICKETS)
                   .withRequestContextValue(PATH, path)
                   .withQueryParameters(Map.of("from", from.toString(), "results", resultSize.toString()))
                   .withRequestContextValue(DOMAIN_NAME, SAMPLE_DOMAIN_NAME)
                   .withTopLevelCristinOrgId(TOP_LEVEL_CRISTIN_ORG_ID)
                   .build();
    }

    private void setupFakeUriRetriever() {
        uriRetriever = mock(AuthorizedBackendUriRetriever.class);
        when(uriRetriever.getRawContent(any(), any())).thenReturn(
            Optional.of(IoUtils.stringFromResources(Path.of("20754.0.0.0.json"))));
    }

    private void prepareSearchClientWithResponse() throws IOException {
        RestHighLevelClient restHighLevelClientMock = mock(RestHighLevelClient.class);
        when(restHighLevelClientMock.search(any(), any())).thenReturn(getSearchResponse());
        restHighLevelClientWrapper = new FakeRestHighLevelClientWrapper(restHighLevelClientMock);
    }

    private InputStream queryWithoutQueryParameters(String path) throws JsonProcessingException {
        var customerId = randomUri();
        return new HandlerRequestBuilder<Void>(objectMapperWithEmpty)
                   .withUserName(USERNAME)
                   .withHeaders(defaultQueryHeaders())
                   .withCurrentCustomer(customerId)
                   .withAccessRights(customerId, ACCESS_RIGHTS_TO_VIEW_TICKETS)
                   .withRequestContextValue(PATH, path)
                   .withRequestContextValue(DOMAIN_NAME, SAMPLE_DOMAIN_NAME)
                   .withTopLevelCristinOrgId(TOP_LEVEL_CRISTIN_ORG_ID)
                   .build();
    }

    private InputStream queryWithoutQueryParameters() throws JsonProcessingException {
        return queryWithoutQueryParameters(MESSAGES_PATH);
    }

    private InputStream queryWithRole(String role) throws JsonProcessingException {
        var customerId = randomUri();
        return new HandlerRequestBuilder<Void>(objectMapperWithEmpty)
                   .withUserName(USERNAME)
                   .withCurrentCustomer(customerId)
                   .withAccessRights(customerId, AccessRight.USER.toString())
                   .withTopLevelCristinOrgId(CUSTOMER_CRISTIN_ID)
                   .withRequestContextValue(PATH, SAMPLE_PATH)
                   .withRequestContextValue(DOMAIN_NAME, SAMPLE_DOMAIN_NAME)
                   .withQueryParameters(Map.of(ROLE, role))
                   .build();
    }

    private InputStream queryWithoutAppropriateAccessRight() throws JsonProcessingException {
        var customerId = randomUri();
        return new HandlerRequestBuilder<Void>(objectMapperWithEmpty)
                   .withUserName(USERNAME)
                   .withCurrentCustomer(customerId)
                   .withAccessRights(customerId, "SomeOtherAccessRight")
                   .withTopLevelCristinOrgId(CUSTOMER_CRISTIN_ID)
                   .withRequestContextValue(PATH, MESSAGES_PATH)
                   .withRequestContextValue(DOMAIN_NAME, SAMPLE_DOMAIN_NAME)
                   .build();
    }

    private Map<String, String> defaultQueryHeaders() {
        return Map.of(HttpHeaders.AUTHORIZATION, randomString());
    }

    private InputStream queryWithCustomOrganizationAsQueryParameter(List<URI> desiredOrgUris)
        throws JsonProcessingException {
        var customerId = randomUri();
        var viewingScope = desiredOrgUris.stream().map(URI::toString).collect(Collectors.joining(COMMA));
        return new HandlerRequestBuilder<Void>(objectMapperWithEmpty)
                   .withQueryParameters(Map.of(VIEWING_SCOPE_KEY, viewingScope))
                   .withUserName(USERNAME)
                   .withCurrentCustomer(customerId)
                   .withAccessRights(customerId, ACCESS_RIGHTS_TO_VIEW_TICKETS)
                   .withTopLevelCristinOrgId(CUSTOMER_CRISTIN_ID)
                   .withRequestContextValue(PATH, MESSAGES_PATH)
                   .withRequestContextValue(DOMAIN_NAME, SAMPLE_DOMAIN_NAME)
                   .build();
    }

    private SearchResponse getSearchResponse() throws IOException {
        String jsonResponse = stringFromResources(Path.of(SAMPLE_OPENSEARCH_TICKETS_RESPONSE_JSON));
        return SearchResponseUtil.getSearchResponseFromJson(jsonResponse);
    }

    private InputStream getInputStream() throws JsonProcessingException {
        var customerId = randomUri();
        return new HandlerRequestBuilder<Void>(objectMapperWithEmpty)
                   .withUserName(USERNAME)
                   .withHeaders(defaultQueryHeaders())
                   .withCurrentCustomer(customerId)
                   .withAccessRights(customerId, ACCESS_RIGHTS_TO_VIEW_TICKETS)
                   .withRequestContextValue(PATH, SAMPLE_PATH)
                   .withQueryParameters(Map.of(SEARCH_TERM_KEY, SAMPLE_SEARCH_TERM))
                   .withRequestContextValue(DOMAIN_NAME, SAMPLE_DOMAIN_NAME)
                   .withTopLevelCristinOrgId(TOP_LEVEL_CRISTIN_ORG_ID)
                   .build();
    }
}
