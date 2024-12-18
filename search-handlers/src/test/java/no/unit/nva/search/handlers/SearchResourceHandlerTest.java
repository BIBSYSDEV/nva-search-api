package no.unit.nva.search.handlers;

import static no.unit.nva.auth.uriretriever.UriRetriever.ACCEPT;
import static no.unit.nva.search.model.constant.Defaults.objectMapperWithEmpty;
import static no.unit.nva.search.model.constant.Words.ASTERISK;
import static no.unit.nva.search.service.ticket.TicketParameter.SEARCH_ALL;

import static nva.commons.core.ioutils.IoUtils.stringFromResources;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import static java.net.HttpURLConnection.HTTP_OK;

import com.amazonaws.services.lambda.runtime.Context;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.node.ObjectNode;

import no.unit.nva.search.FakeGatewayResponse;
import no.unit.nva.search.model.records.SwsResponse;
import no.unit.nva.search.service.resource.ResourceClient;
import no.unit.nva.search.service.resource.response.ResourceSearchResponse;
import no.unit.nva.testutils.HandlerRequestBuilder;

import nva.commons.core.Environment;

import org.hamcrest.CoreMatchers;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.nio.file.Path;
import java.util.Map;

class SearchResourceHandlerTest {

    public static final String SAMPLE_PATH = "search";
    public static final String SAMPLE_DOMAIN_NAME = "localhost";
    public static final String SAMPLE_OPENSEARCH_RESPONSE_WITH_AGGREGATION_JSON =
            "sample_opensearch_response.json";

    private SearchResourceHandler handler;
    private ByteArrayOutputStream outputStream;
    private Context contextMock;
    private ResourceClient mockedSearchClient;

    @BeforeEach
    void setUp() {
        mockedSearchClient = mock(ResourceClient.class);
        handler = new SearchResourceHandler(new Environment(), mockedSearchClient);
        contextMock = mock(Context.class);
        outputStream = new ByteArrayOutputStream();
    }

    @Test
    void shouldReturnContentIn20241201Format() throws IOException {
        prepareRestHighLevelClientOkResponse();
        handler.handleRequest(
                getRequestInputStreamAccepting("application/json;version=2024-12-01"),
                outputStream,
                contextMock);

        var gatewayResponse = FakeGatewayResponse.of(outputStream);

        assertThat(gatewayResponse.statusCode(), is(equalTo(HTTP_OK)));

        var actualBody = gatewayResponse.body();
        var firstHitJson = actualBody.hits().getFirst();
        assertNotNull(firstHitJson);

        var firstHitDto =
                objectMapperWithEmpty.treeToValue(firstHitJson, ResourceSearchResponse.class);
        assertNotNull(firstHitDto);
        assertThat(firstHitDto.otherIdentifiers(), CoreMatchers.notNullValue());

        var uri = URI.create("http://localhost/publication/f367b260-c15e-4d0f-b197-e1dc0e9eb0e8");
        assertThat(firstHitDto.id(), is(equalTo(uri)));
    }

    @Test
    void shouldReturnContentInLegacyFormat() throws IOException {
        prepareRestHighLevelClientOkResponse();

        handler.handleRequest(
                getRequestInputStreamAccepting("application/json"), outputStream, contextMock);

        var gatewayResponse = FakeGatewayResponse.of(outputStream);

        assertThat(gatewayResponse.statusCode(), is(equalTo(HTTP_OK)));

        var actualBody = gatewayResponse.body();
        var firstHitJson = actualBody.hits().getFirst();
        assertNotNull(firstHitJson);
        assertNotNull(firstHitJson.get("entityDescription"));

        assertThat(
                firstHitJson.get("id").asText(),
                is("http://localhost/publication/f367b260-c15e-4d0f-b197-e1dc0e9eb0e8"));
    }

    private InputStream getRequestInputStreamAccepting(String contentType)
            throws JsonProcessingException {
        return new HandlerRequestBuilder<Void>(objectMapperWithEmpty)
                .withQueryParameters(Map.of(SEARCH_ALL.asCamelCase(), ASTERISK))
                .withHeaders(Map.of(ACCEPT, contentType))
                .withRequestContext(getRequestContext())
                .build();
    }

    private ObjectNode getRequestContext() {
        return objectMapperWithEmpty.convertValue(
                Map.of("path", SAMPLE_PATH, "domainName", SAMPLE_DOMAIN_NAME), ObjectNode.class);
    }

    private void prepareRestHighLevelClientOkResponse() throws IOException {
        var jsonResponse =
                stringFromResources(Path.of(SAMPLE_OPENSEARCH_RESPONSE_WITH_AGGREGATION_JSON));
        var body = objectMapperWithEmpty.readValue(jsonResponse, SwsResponse.class);

        when(mockedSearchClient.doSearch(any())).thenReturn(body);
    }
}
