package no.unit.nva.search2;

import com.amazonaws.services.lambda.runtime.Context;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.node.ObjectNode;
import no.unit.nva.search2.model.TestGatewayResponse;
import no.unit.nva.search2.model.PagedSearchResponseDto;
import no.unit.nva.search2.model.OpenSearchSwsResponse;
import no.unit.nva.testutils.HandlerRequestBuilder;
import nva.commons.apigateway.exceptions.BadGatewayException;
import nva.commons.core.Environment;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.util.Map;
import java.util.stream.Stream;

import static java.net.HttpURLConnection.HTTP_BAD_GATEWAY;
import static java.net.HttpURLConnection.HTTP_OK;
import static java.util.Objects.nonNull;
import static no.unit.nva.search2.ResourceParameter.SEARCH_ALL;
import static no.unit.nva.search2.ResourceParameter.SORT_ORDER;
import static no.unit.nva.search2.constant.Defaults.objectMapperWithEmpty;
import static no.unit.nva.testutils.RandomDataGenerator.randomString;
import static nva.commons.core.ioutils.IoUtils.stringFromResources;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.collection.IsEmptyCollection.empty;
import static org.hamcrest.core.Is.is;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class SwsResourceHandlerTest {

    public static final String SAMPLE_PATH = "search";
    public static final String SAMPLE_DOMAIN_NAME = "localhost";
    public static final String SAMPLE_SEARCH_TERM = "searchTerm";
    public static final String SAMPLE_OPENSEARCH_RESPONSE_WITH_AGGREGATION_JSON = "sample_opensearch_response.json";
    public static final String ROUNDTRIP_RESPONSE_JSON = "roundtripResponse.json";
    public static final String EMPTY_OPENSEARCH_RESPONSE_JSON = "empty_opensearch_response.json";
    private SwsPagedResourceHandler handler;
    private Context contextMock;
    private ByteArrayOutputStream outputStream;
    private SwsOpenSearchClient mockedSearchClient;

    @BeforeEach
    void setUp() {

        mockedSearchClient = mock(SwsOpenSearchClient.class);
        handler = new SwsPagedResourceHandler(new Environment(), mockedSearchClient);
        contextMock = mock(Context.class);
        outputStream = new ByteArrayOutputStream();
    }

    @Test
    void shouldReturnSearchResultsWhenQueryIsSingleTerm() throws IOException, BadGatewayException {
        prepareRestHighLevelClientOkResponse();

        handler.handleRequest(getInputStream(), outputStream, contextMock);

        var gatewayResponse = TestGatewayResponse.of(outputStream);
        var actualBody = gatewayResponse.body();
        var expected = getSearchResourcesResponseFromFile(ROUNDTRIP_RESPONSE_JSON);

        assertNotNull(gatewayResponse.headers());
        assertEquals(HTTP_OK, gatewayResponse.statusCode());
        assertThat(actualBody.hits(), is(equalTo(expected.hits())));
    }

    @Test
    void shouldReturnSortedSearchResultsWhenSendingContributorId() throws IOException, BadGatewayException {
        prepareRestHighLevelClientOkResponse();
        handler.handleRequest(getInputStreamWithContributorId(), outputStream, contextMock);

        var gatewayResponse = TestGatewayResponse.of(outputStream);

        assertNotNull(gatewayResponse.headers());
        assertEquals(HTTP_OK, gatewayResponse.statusCode());
    }

    @Test
    void shouldSearchResultsWhenSendingContributorIdAndBadResponseFromPreferencesApi()
        throws IOException, BadGatewayException {
        prepareRestHighLevelClientOkResponse();
        handler.handleRequest(getInputStreamWithContributorId(), outputStream, contextMock);

        var gatewayResponse = TestGatewayResponse.of(outputStream);

        assertNotNull(gatewayResponse.headers());
        assertEquals(HTTP_OK, gatewayResponse.statusCode());
    }

    @Test
    void shouldNotReturnSortedSearchResultsWhenSendingMultipleContributorId()
        throws IOException, BadGatewayException {
        prepareRestHighLevelClientOkResponse();

        handler.handleRequest(getInputStreamWithMultipleContributorId(), outputStream, contextMock);

        var gatewayResponse = TestGatewayResponse.of(outputStream);

        assertNotNull(gatewayResponse.headers());
        assertEquals(HTTP_OK, gatewayResponse.statusCode());
    }

    @Test
    @Disabled
    void shouldReturnAggregationAsPartOfResponseWhenDoingASearch() throws IOException, BadGatewayException {
        prepareRestHighLevelClientOkResponse();

        handler.handleRequest(getInputStream(), outputStream, contextMock);

        var gatewayResponse = TestGatewayResponse.of(outputStream);
        var expected = getSearchResourcesResponseFromFile(ROUNDTRIP_RESPONSE_JSON);
        var actualBody = gatewayResponse.body();


        assertNotNull(gatewayResponse.headers());
        assertEquals(HTTP_OK, gatewayResponse.statusCode());
        assertThat(actualBody, is(equalTo(expected)));
        assertNotNull(actualBody.aggregations());
        assertNotNull(actualBody.aggregations().get("Bidragsyter"));
    }

    @Test
    void shouldReturnSearchResultsWithEmptyHitsWhenQueryResultIsEmpty() throws IOException, BadGatewayException {
        prepareRestHighLevelClientEmptyResponse();

        handler.handleRequest(getInputStream(), outputStream, mock(Context.class));

        var gatewayResponse = TestGatewayResponse.of(outputStream);
        var actualBody = gatewayResponse.body();

        assertNotNull(gatewayResponse.headers());
        assertEquals(HTTP_OK, gatewayResponse.statusCode());
        assertThat(actualBody.totalHits(), is(equalTo(0L)));
        assertThat(actualBody.hits(), is(empty()));
        assertDoesNotThrow(() -> gatewayResponse.body().id().normalize());
    }

    @Test
    void shouldReturn200WhenSortOrderIsNotSpecified() throws IOException, BadGatewayException {
        prepareRestHighLevelClientEmptyResponseForSortOrder();

        handler.handleRequest(getInputStream(), outputStream, mock(Context.class));

        var gatewayResponse = TestGatewayResponse.of(outputStream);
        var actualBody = gatewayResponse.body();

        assertNotNull(gatewayResponse.headers());
        assertEquals(HTTP_OK, gatewayResponse.statusCode());
        assertThat(actualBody.totalHits(), is(equalTo(0L)));
        assertThat(actualBody.hits(), is(empty()));
        assertDoesNotThrow(() -> gatewayResponse.body().id().normalize());
    }

    @Test
    @Disabled
    void shouldReturn200WhenSortOrderIsDescInQueryParameters() throws IOException, BadGatewayException {
        prepareRestHighLevelClientEmptyResponseForSortOrder();

        var queryParameters = Map.of(SEARCH_ALL.key(), SAMPLE_SEARCH_TERM, SORT_ORDER.key(), "desc");

        var inputStream = new HandlerRequestBuilder<Void>(objectMapperWithEmpty).withQueryParameters(queryParameters)
                              .withRequestContext(getRequestContext())
                              .build();

        handler.handleRequest(inputStream, outputStream, mock(Context.class));

        var gatewayResponse = TestGatewayResponse.of(outputStream);
        var paged = gatewayResponse.body();

        assertNotNull(gatewayResponse.headers());
        assertEquals(HTTP_OK, gatewayResponse.statusCode());
        assertThat(paged.totalHits(), is(equalTo(0L)));
        assertThat(paged.hits(), is(empty()));
        assertDoesNotThrow(() -> gatewayResponse.body().id().normalize());
    }

    @Test
    @Disabled
    void shouldReturn200WhenSortOrderIsAscInQueryParameters() throws IOException, BadGatewayException {
        prepareRestHighLevelClientEmptyResponseForSortOrder();

        var queryParameters = Map.of(SEARCH_ALL.key(), SAMPLE_SEARCH_TERM, SORT_ORDER.key(), "asc");

        var inputStream = new HandlerRequestBuilder<Void>(objectMapperWithEmpty).withQueryParameters(queryParameters)
                              .withRequestContext(getRequestContext())
                              .build();

        handler.handleRequest(inputStream, outputStream, mock(Context.class));

        var gatewayResponse = TestGatewayResponse.of(outputStream);
        var paged = gatewayResponse.body();
        assertNotNull(gatewayResponse.headers());
        assertEquals(HTTP_OK, gatewayResponse.statusCode());
        assertThat(paged.totalHits(), is(equalTo(0L)));
        assertThat(paged.hits(), is(empty()));
        assertDoesNotThrow(() -> gatewayResponse.body().id().normalize());
    }

    @Test
    @Disabled
    void shouldReturnBadGatewayResponseWhenNoResponseFromService() throws IOException, BadGatewayException {
        prepareRestHighLevelClientEmptyResponse();

        handler.handleRequest(getInputStream(), outputStream, mock(Context.class));

        var gatewayResponse = TestGatewayResponse.of(outputStream);


        assertNotNull(gatewayResponse.headers());
        assertEquals(HTTP_BAD_GATEWAY, gatewayResponse.statusCode());
    }

    @ParameterizedTest(name = "should return application/json for accept header {0}")
    @MethodSource("acceptHeaderValuesProducingApplicationJsonProvider")
    void shouldProduceApplicationJsonWithGivenAcceptHeader(String acceptHeaderValue)
        throws IOException, BadGatewayException {
        prepareRestHighLevelClientOkResponse();
        var requestInput =
            nonNull(acceptHeaderValue) ? getRequestInputStreamAccepting(acceptHeaderValue) : getInputStream();
        handler.handleRequest(requestInput, outputStream, mock(Context.class));

        var gatewayResponse = TestGatewayResponse.of(outputStream);
        assertThat(gatewayResponse.headers().get("Content-Type"), is(equalTo("application/json; charset=utf-8")));
    }

    private InputStream getInputStream() throws JsonProcessingException {
        return new HandlerRequestBuilder<Void>(objectMapperWithEmpty).withQueryParameters(
            Map.of(SEARCH_ALL.key(), SAMPLE_SEARCH_TERM)).withRequestContext(getRequestContext()).build();
    }

    private InputStream getInputStreamWithContributorId() throws JsonProcessingException {
        return new HandlerRequestBuilder<Void>(objectMapperWithEmpty).withQueryParameters(
                Map.of(SEARCH_ALL.key(), "entityDescription.contributors.identity.id:12345",
                    "results","10","from","0"))
                   .withRequestContext(getRequestContext())
                   .withUserName(randomString())
                   .build();
    }

    private InputStream getInputStreamWithMultipleContributorId() throws JsonProcessingException {
        return
            new HandlerRequestBuilder<Void>(objectMapperWithEmpty)
                .withQueryParameters(
                    Map.of(SEARCH_ALL.key(),
                           "((entityDescription.contributors.identity.id:12345)"
                           + "+OR+"
                           + "(entityDescription.contributors.identity.id:54321))"))
                .withRequestContext(getRequestContext())
                .withUserName(randomString())
                .build();
    }

    private InputStream getRequestInputStreamAccepting(String contentType) throws JsonProcessingException {
        return new HandlerRequestBuilder<Void>(objectMapperWithEmpty).withQueryParameters(
                Map.of(SEARCH_ALL.key(), SAMPLE_SEARCH_TERM))
                   .withHeaders(Map.of("Accept", contentType))
                   .withRequestContext(getRequestContext())
                   .build();
    }

    private ObjectNode getRequestContext() {
        return objectMapperWithEmpty.convertValue(
            Map.of("path", SAMPLE_PATH, "domainName", SAMPLE_DOMAIN_NAME), ObjectNode.class);
    }

    private void prepareRestHighLevelClientOkResponse() throws IOException, BadGatewayException {
        var jsonResponse = stringFromResources(Path.of(SAMPLE_OPENSEARCH_RESPONSE_WITH_AGGREGATION_JSON));
        var body = objectMapperWithEmpty.readValue(jsonResponse, OpenSearchSwsResponse.class);

        when(mockedSearchClient.doSearch(any()))
            .thenReturn(body);
    }

    private void prepareRestHighLevelClientEmptyResponse() throws IOException, BadGatewayException {
        var jsonResponse = stringFromResources(Path.of(EMPTY_OPENSEARCH_RESPONSE_JSON));
        var body = objectMapperWithEmpty.readValue(jsonResponse, OpenSearchSwsResponse.class);

        when(mockedSearchClient.doSearch(any()))
            .thenReturn(body);
    }

    private void prepareRestHighLevelClientEmptyResponseForSortOrder() throws IOException, BadGatewayException {
        var jsonResponse = stringFromResources(Path.of(EMPTY_OPENSEARCH_RESPONSE_JSON));
        var body = objectMapperWithEmpty.readValue(jsonResponse, OpenSearchSwsResponse.class);

        when(mockedSearchClient.doSearch(any()))
            .thenReturn(body);
    }

    private PagedSearchResponseDto getSearchResourcesResponseFromFile(String filename) throws JsonProcessingException {
        return objectMapperWithEmpty.readValue(stringFromResources(Path.of(filename)), PagedSearchResponseDto.class);
    }

    public static Stream<String> acceptHeaderValuesProducingApplicationJsonProvider() {
        return Stream.of(null, "application/json");
    }

}