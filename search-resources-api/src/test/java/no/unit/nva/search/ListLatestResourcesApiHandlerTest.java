package no.unit.nva.search;

import com.amazonaws.services.lambda.runtime.Context;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import nva.commons.exceptions.ApiGatewayException;
import nva.commons.handlers.RequestInfo;
import nva.commons.utils.Environment;
import nva.commons.utils.IoUtils;
import nva.commons.utils.JsonUtils;
import org.apache.http.HttpStatus;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.client.RestHighLevelClient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.Executable;

import java.io.IOException;
import java.net.URI;
import java.nio.file.Path;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static no.unit.nva.search.ElasticSearchHighLevelRestClient.ELASTICSEARCH_ENDPOINT_ADDRESS_KEY;
import static no.unit.nva.search.ElasticSearchHighLevelRestClient.ELASTICSEARCH_ENDPOINT_API_SCHEME_KEY;
import static no.unit.nva.search.ElasticSearchHighLevelRestClient.ELASTICSEARCH_ENDPOINT_INDEX_KEY;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class ListLatestResourcesApiHandlerTest {

    public static final String SAMPLE_ELASTICSEARCH_RESPONSE_JSON = "sample_elasticsearch_response.json";
    public static final ObjectMapper mapper = JsonUtils.objectMapper;
    public static final String ROUNDTRIP_RESPONSE_JSON = "roundtripResponse.json";
    public static final URI EXAMPLE_CONTEXT = URI.create("https://example.org/search");
    public static final List<JsonNode> SAMPLE_HITS = Collections.EMPTY_LIST;
    public static final int SAMPLE_TOOK = 0;
    public static final int SAMPLE_TOTAL = 0;
    public static final String SAMPLE_RESULTS = "4";
    private Environment environment;
    private ListLatestResourcesApiHandler listlatestResourcesApiHandler;

    private void initEnvironment() {
        environment = mock(Environment.class);
        when(environment.readEnv(ELASTICSEARCH_ENDPOINT_ADDRESS_KEY)).thenReturn("localhost");
        when(environment.readEnv(ELASTICSEARCH_ENDPOINT_INDEX_KEY)).thenReturn("resources");
        when(environment.readEnv(ELASTICSEARCH_ENDPOINT_API_SCHEME_KEY)).thenReturn("http");
    }

    @BeforeEach
    public void init() {
        initEnvironment();
        listlatestResourcesApiHandler = new ListLatestResourcesApiHandler(environment);
    }

    @Test
    void defaultConstructorThrowsIllegalStateExceptionWhenEnvironmentNotDefined() {
        assertThrows(IllegalStateException.class, ListLatestResourcesApiHandler::new);
    }

    @Test
    void getSuccessStatusCodeReturnsOK() {
        SearchResourcesResponse response =  new SearchResourcesResponse(EXAMPLE_CONTEXT,
                SAMPLE_TOOK,
                SAMPLE_TOTAL,
                SAMPLE_HITS);
        Integer statusCode = listlatestResourcesApiHandler.getSuccessStatusCode(null, response);
        assertEquals(statusCode, HttpStatus.SC_OK);
    }

    @Test
    void handlerReturnsSearchResultsWhenNoParametersIsSet() throws ApiGatewayException, IOException {
        var elasticSearchClient = new ElasticSearchHighLevelRestClient(environment, setUpRestHighLevelClient());
        var handler = new ListLatestResourcesApiHandler(environment, elasticSearchClient);
        var actual = handler.processInput(null, getRequestInfo(), mock(Context.class));
        var expected = mapper.readValue(IoUtils.stringFromResources(Path.of(ROUNDTRIP_RESPONSE_JSON)),
                SearchResourcesResponse.class);
        assertEquals(expected, actual);
    }

    @Test
    void handlerThrowsExceptionWhenGatewayIsBad() throws IOException {
        var elasticSearchClient = new ElasticSearchHighLevelRestClient(environment, setUpBadGateWay());
        var handler = new ListLatestResourcesApiHandler(environment, elasticSearchClient);
        Executable executable = () -> handler.processInput(null, getRequestInfo(), mock(Context.class));
        assertThrows(ApiGatewayException.class, executable);
    }

    private RequestInfo getRequestInfo() {
        var requestInfo = new RequestInfo();
        requestInfo.setQueryParameters(Map.of(RequestUtil.RESULTS_KEY, SAMPLE_RESULTS));
        return requestInfo;
    }

    private RestHighLevelClient setUpRestHighLevelClient() throws IOException {
        String result = IoUtils.stringFromResources(Path.of(SAMPLE_ELASTICSEARCH_RESPONSE_JSON));
        SearchResponse searchResponse = getSearchResponse(result);
        RestHighLevelClient restHighLevelClient = mock(RestHighLevelClient.class);
        when(restHighLevelClient.search(any(), any())).thenReturn(searchResponse);
        return restHighLevelClient;
    }

    private RestHighLevelClient setUpBadGateWay() throws IOException {
        RestHighLevelClient restHighLevelClient = mock(RestHighLevelClient.class);
        when(restHighLevelClient.search(any(), any())).thenThrow(IOException.class);
        return restHighLevelClient;
    }

    private SearchResponse getSearchResponse(String hits) {
        var searchResponse = mock(SearchResponse.class);
        when(searchResponse.toString()).thenReturn(hits);
        return searchResponse;
    }
}
