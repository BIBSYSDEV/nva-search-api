package no.unit.nva.search;

import static no.unit.nva.indexing.testutils.MockedJwtProvider.setupMockedCachedJwtProvider;
import static no.unit.nva.search.RequestUtil.DOMAIN_NAME;
import static no.unit.nva.search.RequestUtil.PATH;
import static no.unit.nva.search.constants.ApplicationConstants.objectMapperWithEmpty;
import static no.unit.nva.search.models.SearchTicketsQuery.VIEWING_SCOPE_QUERY_NAME;
import static no.unit.nva.testutils.RandomDataGenerator.randomString;
import static no.unit.nva.testutils.RandomDataGenerator.randomUri;
import static nva.commons.apigateway.AccessRight.MANAGE_DOI;
import static nva.commons.core.ioutils.IoUtils.stringFromResources;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import com.amazonaws.services.lambda.runtime.Context;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.google.common.net.HttpHeaders;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.nio.file.Path;
import java.util.Map;
import java.util.Optional;
import no.unit.nva.auth.uriretriever.AuthorizedBackendUriRetriever;
import no.unit.nva.indexing.testutils.SearchResponseUtil;
import no.unit.nva.search.restclients.IdentityClient;
import no.unit.nva.testutils.HandlerRequestBuilder;
import nva.commons.core.Environment;
import nva.commons.core.ioutils.IoUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.opensearch.action.search.SearchResponse;
import org.opensearch.client.RestHighLevelClient;

class SearchMyTicketsHandlerTest {
    private static final String SAMPLE_OPENSEARCH_TICKETS_RESPONSE_JSON = "sample_opensearch_mytickets_response.json";
    private static final String OWNER_ID = "1306838@20754.0.0.0";
    private static final String SAMPLE_DOMAIN_NAME = "localhost";
    public static final URI TOP_LEVEL_CRISTIN_ORG_ID = URI.create(
        "https://api.dev.nva.aws.unit.no/cristin/organization/20754.0.0.0");
    private static final String USERNAME = randomString();
    private static final String ROLE = "role";
    private SearchTicketsHandler handler;
    private IdentityClient identityClientMock;
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
    void shouldDefaultViewingScopeToTopLevelCristinOrgIdWhenNoViewingScopeIsProvided() throws IOException {
        var topLevelCristinOrg = randomUri();
        var query = queryWithTopLevelCristinOrg(topLevelCristinOrg);
        handler.handleRequest(query, outputStream, context);

        var searchRequest = restHighLevelClientWrapper.getSearchRequest();
        var queryDescription = searchRequest.buildDescription();
        assertThat(queryDescription, containsString(topLevelCristinOrg.toString()));
    }


    @Test
    void shouldNotSearchForOwnerIfNoRoleIsProvided() throws IOException {
        var query = searchQueryWithoutAnyParameters();
        handler.handleRequest(query, outputStream, context);
        var searchRequest = restHighLevelClientWrapper.getSearchRequest();

        var queryDescription = searchRequest.buildDescription();
        assertThat(queryDescription, not(containsString("owner")));
    }

    @Test
    void shouldNotSearchForOwnerIfRoleCuratorIsProvided() throws IOException {
        var query = searchQueryWithParameters(Map.of(ROLE, "curator"));
        handler.handleRequest(query, outputStream, context);
        var searchRequest = restHighLevelClientWrapper.getSearchRequest();

        var queryDescription = searchRequest.buildDescription();
        assertThat(queryDescription, not(containsString("owner")));
    }

    @Test
    void shouldSearchForOwnerIfRoleCreatorIsProvided() throws IOException {
        var query = searchQueryWithParameters(Map.of(ROLE, "creator"));
        handler.handleRequest(query, outputStream, context);
        var searchRequest = restHighLevelClientWrapper.getSearchRequest();

        var queryDescription = searchRequest.buildDescription();
        assertThat(queryDescription, containsString("owner"));
    }

    @Test
    void shouldIncludeViewingScopeWhenPerformingCuratorSearch() throws IOException {
        var query = searchQueryWithoutAnyParameters();
        handler.handleRequest(query, outputStream, context);
        var searchRequest = restHighLevelClientWrapper.getSearchRequest();

        var queryDescription = searchRequest.buildDescription();
        assertThat(queryDescription, containsString(VIEWING_SCOPE_QUERY_NAME));
    }

    @Test
    void shouldNotIncludeViewingScopeWhenPerformingCreatorSearch() throws IOException {
        var query = searchQueryWithParameters(Map.of(ROLE, "creator"));
        handler.handleRequest(query, outputStream, context);
        var searchRequest = restHighLevelClientWrapper.getSearchRequest();

        var queryDescription = searchRequest.buildDescription();
        assertThat(queryDescription, not(containsString(VIEWING_SCOPE_QUERY_NAME)));
    }

    @Test
    void shouldQueryOnOwnerIfRoleCreator() throws IOException {
        var query = searchQueryWithParameters(Map.of(ROLE, "creator"));
        handler.handleRequest(query, outputStream, context);
        var searchRequest = restHighLevelClientWrapper.getSearchRequest();

        var queryDescription = searchRequest.buildDescription();
        assertThat(queryDescription, containsString(OWNER_ID));
        assertThat(queryDescription, containsString("owner"));
    }

    private InputStream searchQueryWithParameters(Map<String, String> queryParams) throws JsonProcessingException {
        URI customerId = randomUri();
        return new HandlerRequestBuilder<Void>(objectMapperWithEmpty)
            .withQueryParameters(queryParams)
            .withUserName(OWNER_ID)
            .withHeaders(defaultQueryHeaders())
            .withCurrentCustomer(customerId)
            .withAccessRights(customerId, MANAGE_DOI)
            .withRequestContextValue(PATH, "tickets")
            .withRequestContextValue(DOMAIN_NAME, SAMPLE_DOMAIN_NAME)
            .withTopLevelCristinOrgId(TOP_LEVEL_CRISTIN_ORG_ID)
            .build();
    }

    private InputStream searchQueryWithoutAnyParameters() throws JsonProcessingException {
        URI customerId = randomUri();
        return new HandlerRequestBuilder<Void>(objectMapperWithEmpty)
            .withUserName(USERNAME)
            .withHeaders(defaultQueryHeaders())
            .withCurrentCustomer(customerId)
            .withAccessRights(customerId, MANAGE_DOI)
            .withRequestContextValue(PATH, "tickets")
            .withRequestContextValue(DOMAIN_NAME, SAMPLE_DOMAIN_NAME)
            .withTopLevelCristinOrgId(TOP_LEVEL_CRISTIN_ORG_ID)
            .build();
    }

    private InputStream queryWithTopLevelCristinOrg(URI topLevelCristinOrg) throws JsonProcessingException {
        var customerId = randomUri();
        return new HandlerRequestBuilder<Void>(objectMapperWithEmpty)
                   .withUserName(USERNAME)
                   .withHeaders(defaultQueryHeaders())
                   .withCurrentCustomer(customerId)
                   .withAccessRights(customerId, MANAGE_DOI)
                   .withRequestContextValue(PATH, "tickets")
                   .withRequestContextValue(DOMAIN_NAME, SAMPLE_DOMAIN_NAME)
                   .withTopLevelCristinOrgId(TOP_LEVEL_CRISTIN_ORG_ID)
                   .withTopLevelCristinOrgId(topLevelCristinOrg)
                   .build();
    }

    private void prepareSearchClientWithResponse() throws IOException {
        RestHighLevelClient restHighLevelClientMock = mock(RestHighLevelClient.class);
        when(restHighLevelClientMock.search(any(), any())).thenReturn(getSearchResponse());
        restHighLevelClientWrapper = new FakeRestHighLevelClientWrapper(restHighLevelClientMock);
    }

    private SearchResponse getSearchResponse() throws IOException {
        String jsonResponse = stringFromResources(Path.of(SAMPLE_OPENSEARCH_TICKETS_RESPONSE_JSON));
        return SearchResponseUtil.getSearchResponseFromJson(jsonResponse);
    }

    private void setupFakeUriRetriever() {
        uriRetriever = mock(AuthorizedBackendUriRetriever.class);
        when(uriRetriever.getRawContent(any(), any())).thenReturn(
            Optional.of(IoUtils.stringFromResources(Path.of("20754.0.0.0.json"))));
    }

    private Map<String, String> defaultQueryHeaders() {
        return Map.of(HttpHeaders.AUTHORIZATION, randomString());
    }
}
