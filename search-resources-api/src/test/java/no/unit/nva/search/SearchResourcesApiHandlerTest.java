package no.unit.nva.search;

import com.amazonaws.services.lambda.runtime.Context;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.node.ObjectNode;
import no.unit.nva.search.models.SearchResponseDto;
import no.unit.nva.search.utils.SortKeyHttpRequestMatcher;
import no.unit.nva.testutils.HandlerRequestBuilder;
import nva.commons.apigateway.GatewayResponse;
import nva.commons.core.Environment;
import org.opensearch.action.search.SearchResponse;
import org.opensearch.client.RestHighLevelClient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.zalando.problem.Problem;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.util.Map;

import static java.net.HttpURLConnection.HTTP_BAD_GATEWAY;
import static java.net.HttpURLConnection.HTTP_OK;
import static no.unit.nva.indexing.testutils.SearchResponseUtil.getSearchResponseFromJson;
import static no.unit.nva.indexing.testutils.TestSetup.setupMockedCachedJwtProvider;
import static no.unit.nva.search.RequestUtil.DOMAIN_NAME;
import static no.unit.nva.search.RequestUtil.PATH;
import static no.unit.nva.search.RequestUtil.SEARCH_TERM_KEY;
import static no.unit.nva.search.RequestUtil.SORTORDER_KEY;
import static no.unit.nva.search.constants.ApplicationConstants.objectMapperWithEmpty;
import static nva.commons.core.ioutils.IoUtils.stringFromResources;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.collection.IsEmptyCollection.empty;
import static org.hamcrest.core.Is.is;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class SearchResourcesApiHandlerTest {

    public static final String SAMPLE_SEARCH_TERM = "searchTerm";
    public static final String SAMPLE_OPENSEARCH_RESPONSE_WITH_AGGREGATION_JSON
        = "sample_opensearch_response.json";
    public static final String EMPTY_OPENSEARCH_RESPONSE_JSON = "empty_opensearch_response.json";
    public static final String ROUNDTRIP_RESPONSE_JSON = "roundtripResponse.json";
    public static final String SAMPLE_PATH = "search";
    public static final String SAMPLE_DOMAIN_NAME = "localhost";

    private RestHighLevelClient restHighLevelClientMock;
    private SearchResourcesApiHandler handler;
    private Context contextMock;
    private ByteArrayOutputStream outputStream;

    @BeforeEach
    void init() {
        restHighLevelClientMock = mock(RestHighLevelClient.class);
        var cachedJwtProvider = setupMockedCachedJwtProvider();
        var restHighLevelClientWrapper = new RestHighLevelClientWrapper(restHighLevelClientMock);
        var searchClient = new SearchClient(restHighLevelClientWrapper, cachedJwtProvider);
        handler = new SearchResourcesApiHandler(new Environment(), searchClient);
        contextMock = mock(Context.class);
        outputStream = new ByteArrayOutputStream();
    }

    @Test
    void shouldReturnSearchResultsWhenQueryIsSingleTerm() throws IOException {
        prepareRestHighLevelClientOkResponse();

        handler.handleRequest(getInputStream(), outputStream, contextMock);

        var gatewayResponse = GatewayResponse.fromOutputStream(outputStream, SearchResponseDto.class);
        SearchResponseDto actual = gatewayResponse.getBodyObject(SearchResponseDto.class);

        SearchResponseDto expected = getSearchResourcesResponseFromFile(ROUNDTRIP_RESPONSE_JSON);

        assertNotNull(gatewayResponse.getHeaders());
        assertEquals(HTTP_OK, gatewayResponse.getStatusCode());
        assertThat(actual, is(equalTo(expected)));
    }

    @Test
    void shouldReturnAggregationAsPartOfResponseWhenDoingASearch() throws IOException {
        prepareRestHighLevelClientOkResponse();

        handler.handleRequest(getInputStream(), outputStream, contextMock);

        var gatewayResponse = GatewayResponse
            .fromOutputStream(outputStream, SearchResponseDto.class);

        SearchResponseDto actual = gatewayResponse.getBodyObject(SearchResponseDto.class);

        SearchResponseDto expected = getSearchResourcesResponseFromFile(ROUNDTRIP_RESPONSE_JSON);

        assertNotNull(gatewayResponse.getHeaders());
        assertEquals(HTTP_OK, gatewayResponse.getStatusCode());
        assertThat(actual, is(equalTo(expected)));
        assertNotNull(actual.getAggregations());
        assertNotNull(actual.getAggregations().get("Bidragsyter"));
    }

    @Test
    void shouldReturnSearchResultsWithEmptyHitsWhenQueryResultIsEmpty() throws IOException {
        prepareRestHighLevelClientEmptyResponse();

        handler.handleRequest(getInputStream(), outputStream, mock(Context.class));

        var gatewayResponse = GatewayResponse.fromOutputStream(outputStream, SearchResponseDto.class);
        var body = gatewayResponse.getBodyObject(SearchResponseDto.class);

        assertNotNull(gatewayResponse.getHeaders());
        assertEquals(HTTP_OK, gatewayResponse.getStatusCode());
        assertThat(body.getSize(), is(equalTo(0L)));
        assertThat(body.getHits(), is(empty()));
        assertDoesNotThrow(() -> body.getId().normalize());
    }

    @Test
    void shouldReturn200WhenSortOrderIsNotSpecified() throws IOException {
        prepareRestHighLevelClientEmptyResponseForSortOrder("desc");

        handler.handleRequest(getInputStream(), outputStream, mock(Context.class));

        var gatewayResponse = GatewayResponse.fromOutputStream(outputStream, SearchResponseDto.class);
        var body = gatewayResponse.getBodyObject(SearchResponseDto.class);

        assertNotNull(gatewayResponse.getHeaders());
        assertEquals(HTTP_OK, gatewayResponse.getStatusCode());
        assertThat(body.getSize(), is(equalTo(0L)));
        assertThat(body.getHits(), is(empty()));
        assertDoesNotThrow(() -> body.getId().normalize());
    }

    @Test
    void shouldReturn200WhenSortOrderIsDescInQueryParameters() throws IOException {
        prepareRestHighLevelClientEmptyResponseForSortOrder("desc");

        var queryParameters = Map.of(
            SEARCH_TERM_KEY, SAMPLE_SEARCH_TERM, SORTORDER_KEY, "desc"
        );

        var inputStream = new HandlerRequestBuilder<Void>(objectMapperWithEmpty)
                              .withQueryParameters(queryParameters)
                              .withRequestContext(getRequestContext())
                              .build();

        handler.handleRequest(inputStream, outputStream, mock(Context.class));

        var gatewayResponse = GatewayResponse.fromOutputStream(outputStream, SearchResponseDto.class);
        var body = gatewayResponse.getBodyObject(SearchResponseDto.class);

        assertNotNull(gatewayResponse.getHeaders());
        assertEquals(HTTP_OK, gatewayResponse.getStatusCode());
        assertThat(body.getSize(), is(equalTo(0L)));
        assertThat(body.getHits(), is(empty()));
        assertDoesNotThrow(() -> body.getId().normalize());
    }

    @Test
    void shouldReturn200WhenSortOrderIsAscInQueryParameters() throws IOException {
        prepareRestHighLevelClientEmptyResponseForSortOrder("asc");

        var queryParameters = Map.of(
            SEARCH_TERM_KEY, SAMPLE_SEARCH_TERM, SORTORDER_KEY, "asc"
        );

        var inputStream = new HandlerRequestBuilder<Void>(objectMapperWithEmpty)
                              .withQueryParameters(queryParameters)
                              .withRequestContext(getRequestContext())
                              .build();

        handler.handleRequest(inputStream, outputStream, mock(Context.class));

        var gatewayResponse = GatewayResponse.fromOutputStream(outputStream, SearchResponseDto.class);
        var body = gatewayResponse.getBodyObject(SearchResponseDto.class);

        assertNotNull(gatewayResponse.getHeaders());
        assertEquals(HTTP_OK, gatewayResponse.getStatusCode());
        assertThat(body.getSize(), is(equalTo(0L)));
        assertThat(body.getHits(), is(empty()));
        assertDoesNotThrow(() -> body.getId().normalize());
    }

    @Test
    void shouldReturnBadGatewayResponseWhenNoResponseFromService() throws IOException {
        prepareRestHighLevelClientNoResponse();

        handler.handleRequest(getInputStream(), outputStream, mock(Context.class));

        GatewayResponse<Problem> gatewayResponse = GatewayResponse.fromOutputStream(outputStream, Problem.class);

        assertNotNull(gatewayResponse.getHeaders());
        assertEquals(HTTP_BAD_GATEWAY, gatewayResponse.getStatusCode());
    }

    private InputStream getInputStream() throws JsonProcessingException {
        return new HandlerRequestBuilder<Void>(objectMapperWithEmpty)
            .withQueryParameters(Map.of(SEARCH_TERM_KEY, SAMPLE_SEARCH_TERM))
            .withRequestContext(getRequestContext())
            .build();
    }

    private ObjectNode getRequestContext() {
        return objectMapperWithEmpty.convertValue(Map.of(PATH, SAMPLE_PATH, DOMAIN_NAME, SAMPLE_DOMAIN_NAME),
                                                  ObjectNode.class);
    }

    private void prepareRestHighLevelClientOkResponse() throws IOException {
        String result = stringFromResources(Path.of(SAMPLE_OPENSEARCH_RESPONSE_WITH_AGGREGATION_JSON));
        SearchResponse searchResponse = createSearchResponseWithHits(SAMPLE_OPENSEARCH_RESPONSE_WITH_AGGREGATION_JSON);

        when(restHighLevelClientMock.search(any(), any())).thenReturn(searchResponse);
    }

    private void prepareRestHighLevelClientEmptyResponse() throws IOException {
        SearchResponse searchResponse = createSearchResponseWithHits(EMPTY_OPENSEARCH_RESPONSE_JSON);

        when(restHighLevelClientMock.search(any(), any())).thenReturn(searchResponse);
    }

    private void prepareRestHighLevelClientEmptyResponseForSortOrder(String sortOrder) throws IOException {
        SearchResponse searchResponse = createSearchResponseWithHits(EMPTY_OPENSEARCH_RESPONSE_JSON);
        when(
            restHighLevelClientMock.search(argThat(new SortKeyHttpRequestMatcher(sortOrder)), any())
        ).thenReturn(searchResponse);
    }

    private void prepareRestHighLevelClientNoResponse() throws IOException {
        when(restHighLevelClientMock.search(any(), any())).thenThrow(IOException.class);
    }

    private SearchResponseDto getSearchResourcesResponseFromFile(String filename)
        throws JsonProcessingException {
        return objectMapperWithEmpty
            .readValue(stringFromResources(Path.of(filename)), SearchResponseDto.class);
    }

    private SearchResponse createSearchResponseWithHits(String responseJsonFile) throws IOException {
        String jsonResponse = stringFromResources(Path.of(responseJsonFile));
        return getSearchResponseFromJson(jsonResponse);
    }
}
