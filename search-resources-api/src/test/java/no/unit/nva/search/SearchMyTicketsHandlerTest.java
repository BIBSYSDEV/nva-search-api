package no.unit.nva.search;

import com.amazonaws.services.lambda.runtime.Context;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.google.common.net.HttpHeaders;
import no.unit.nva.indexing.testutils.SearchResponseUtil;
import no.unit.nva.search.restclients.IdentityClient;
import no.unit.nva.search.restclients.responses.UserResponse;
import no.unit.nva.search.restclients.responses.ViewingScope;
import no.unit.nva.testutils.HandlerRequestBuilder;
import nva.commons.core.Environment;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.opensearch.action.search.SearchResponse;
import org.opensearch.client.RestHighLevelClient;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.nio.file.Path;
import java.util.Collections;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import static no.unit.nva.indexing.testutils.TestSetup.setupMockedCachedJwtProvider;
import static no.unit.nva.search.RequestUtil.DOMAIN_NAME;
import static no.unit.nva.search.RequestUtil.PATH;
import static no.unit.nva.search.SearchTicketsHandler.ACCESS_RIGHTS_TO_VIEW_TICKETS;
import static no.unit.nva.search.constants.ApplicationConstants.objectMapperWithEmpty;
import static no.unit.nva.testutils.RandomDataGenerator.randomString;
import static no.unit.nva.testutils.RandomDataGenerator.randomUri;
import static nva.commons.core.ioutils.IoUtils.stringFromResources;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class SearchMyTicketsHandlerTest {
    private static final String SAMPLE_OPENSEARCH_TICKETS_RESPONSE_JSON = "sample_opensearch_mytickets_response.json";
    private static final String OWNER_ID = "1306838@20754.0.0.0";
    private static final String SAMPLE_DOMAIN_NAME = "localhost";
    private static final String USERNAME = randomString();
    private static final String ROLE = "role";
    private SearchTicketsHandler handler;
    private IdentityClient identityClientMock;
    private Context context;
    private ByteArrayOutputStream outputStream;
    private FakeRestHighLevelClientWrapper restHighLevelClientWrapper;

    @BeforeEach
    void init() throws IOException {
        var cachedJwtProvider = setupMockedCachedJwtProvider();
        prepareSearchClientWithResponse();
        var searchClient = new SearchClient(restHighLevelClientWrapper, cachedJwtProvider);
        setupFakeIdentityClient();
        handler = new SearchTicketsHandler(new Environment(), searchClient, identityClientMock);
        context = mock(Context.class);
        outputStream = new ByteArrayOutputStream();
    }

    @Test
    void shouldDefaultToCuratorRoleIfNoRoleIsProvided() throws IOException {
        var query = searchQueryWithoutAnyParameters();
        handler.handleRequest(query, outputStream, context);
        var searchRequest = restHighLevelClientWrapper.getSearchRequest();

        var queryDescription = searchRequest.buildDescription();
        for (var uriInDefaultViewingScope : includedUrisInDefaultViewingScope()) {
            assertThat(queryDescription, containsString(uriInDefaultViewingScope.toString()));
        }
    }

    @Test
    void shouldReturnCuratorTicketsIfCuratorRoleIsSet() throws IOException {
        var query = searchQueryWithParameters(Map.of(ROLE, "curator"));
        handler.handleRequest(query, outputStream, context);
        var searchRequest = restHighLevelClientWrapper.getSearchRequest();

        var queryDescription = searchRequest.buildDescription();
        for (var uriInDefaultViewingScope : includedUrisInDefaultViewingScope()) {
            assertThat(queryDescription, containsString(uriInDefaultViewingScope.toString()));
        }
    }

    @Test
    void shouldNotContainOrgIfRoleCreatorIsProvided() throws IOException {
        var query = searchQueryWithParameters(Map.of(ROLE, "creator"));
        handler.handleRequest(query, outputStream, context);
        var searchRequest = restHighLevelClientWrapper.getSearchRequest();

        var queryDescription = searchRequest.buildDescription();
        for (var uriInDefaultViewingScope : includedUrisInDefaultViewingScope()) {
            assertThat(queryDescription, not(containsString(uriInDefaultViewingScope.toString())));
        }
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
            .withAccessRights(customerId, ACCESS_RIGHTS_TO_VIEW_TICKETS)
            .withRequestContextValue(PATH, "tickets")
            .withRequestContextValue(DOMAIN_NAME, SAMPLE_DOMAIN_NAME)
            .build();
    }

    private Set<URI> includedUrisInDefaultViewingScope() {
        return identityClientMock.getUser(USERNAME, randomString())
            .map(UserResponse::getViewingScope)
            .map(ViewingScope::getIncludedUnits)
            .orElseThrow();
    }

    private InputStream searchQueryWithoutAnyParameters() throws JsonProcessingException {
        URI customerId = randomUri();
        return new HandlerRequestBuilder<Void>(objectMapperWithEmpty)
            .withUserName(USERNAME)
            .withHeaders(defaultQueryHeaders())
            .withCurrentCustomer(customerId)
            .withAccessRights(customerId, ACCESS_RIGHTS_TO_VIEW_TICKETS)
            .withRequestContextValue(PATH, "tickets")
            .withRequestContextValue(DOMAIN_NAME, SAMPLE_DOMAIN_NAME)
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

    private void setupFakeIdentityClient() {
        identityClientMock = mock(IdentityClient.class);
        when(identityClientMock.getUser(anyString(), anyString())).thenReturn(getUserResponse());
    }

    private Optional<UserResponse> getUserResponse() {
        UserResponse userResponse = new UserResponse();
        ViewingScope viewingScope = new ViewingScope();
        viewingScope.setIncludedUnits(Set.of(randomUri(), randomUri()));
        viewingScope.setExcludedUnits(Collections.emptySet());
        userResponse.setViewingScope(viewingScope);
        return Optional.of(userResponse);
    }

    private Map<String, String> defaultQueryHeaders() {
        return Map.of(HttpHeaders.AUTHORIZATION, randomString());
    }
}
