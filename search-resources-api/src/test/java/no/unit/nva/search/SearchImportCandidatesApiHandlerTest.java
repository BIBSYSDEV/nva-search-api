package no.unit.nva.search;

import static java.net.HttpURLConnection.HTTP_OK;
import static no.unit.nva.indexing.testutils.SearchResponseUtil.getSearchResponseFromJson;
import static no.unit.nva.indexing.testutils.MockedJwtProvider.setupMockedCachedJwtProvider;
import static no.unit.nva.search.RequestUtil.DOMAIN_NAME;
import static no.unit.nva.search.RequestUtil.PATH;
import static no.unit.nva.search.RequestUtil.SEARCH_TERM_KEY;
import static no.unit.nva.search.constants.ApplicationConstants.objectMapperWithEmpty;
import static nva.commons.core.ioutils.IoUtils.stringFromResources;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import com.amazonaws.services.lambda.runtime.Context;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.util.Map;
import no.unit.nva.search.models.SearchResponseDto;
import no.unit.nva.testutils.HandlerRequestBuilder;
import nva.commons.apigateway.GatewayResponse;
import nva.commons.core.Environment;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.opensearch.action.search.SearchResponse;
import org.opensearch.client.RestHighLevelClient;

public class SearchImportCandidatesApiHandlerTest {

    public static final String SAMPLE_OPENSEARCH_RESPONSE
        = "sample_opensearch_importCandidate_response.json";
    public static final String ROUNDTRIP_RESPONSE_JSON = "roundTripImportCandidateResponse.json";
    public static final String SAMPLE_SEARCH_TERM = "searchTerm";
    public static final String SAMPLE_PATH = "search";
    public static final String SAMPLE_DOMAIN_NAME = "localhost";
    private RestHighLevelClient restHighLevelClientMock;
    private SearchImportCandidatesApiHandler handler;
    private Context contextMock;
    private ByteArrayOutputStream outputStream;

    @BeforeEach
    void init() {
        restHighLevelClientMock = mock(RestHighLevelClient.class);
        var cachedJwtProvider = setupMockedCachedJwtProvider();
        var restHighLevelClientWrapper = new RestHighLevelClientWrapper(restHighLevelClientMock);
        var searchClient = new SearchClient(restHighLevelClientWrapper, cachedJwtProvider);
        handler = new SearchImportCandidatesApiHandler(new Environment(), searchClient);
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

    private SearchResponseDto getSearchResourcesResponseFromFile(String filename)
        throws JsonProcessingException {
        return objectMapperWithEmpty
                   .readValue(stringFromResources(Path.of(filename)), SearchResponseDto.class);
    }

    private void prepareRestHighLevelClientOkResponse() throws IOException {
        String result = stringFromResources(Path.of(SAMPLE_OPENSEARCH_RESPONSE));
        SearchResponse searchResponse = createSearchResponseWithHits(SAMPLE_OPENSEARCH_RESPONSE);

        when(restHighLevelClientMock.search(any(), any())).thenReturn(searchResponse);
    }

    private SearchResponse createSearchResponseWithHits(String response) throws IOException {
        String jsonResponse = stringFromResources(Path.of(response));
        return getSearchResponseFromJson(jsonResponse);
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
}
