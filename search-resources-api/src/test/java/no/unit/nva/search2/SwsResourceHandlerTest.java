package no.unit.nva.search2;

import com.amazonaws.services.lambda.runtime.Context;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.node.ObjectNode;
import no.unit.nva.search.models.SearchResponseDto;
import no.unit.nva.search2.common.OpenSearchResponseDto;
import no.unit.nva.testutils.HandlerRequestBuilder;
import nva.commons.apigateway.GatewayResponse;
import nva.commons.core.Environment;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.zalando.problem.Problem;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.nio.file.Path;
import java.util.Map;
import java.util.stream.Stream;

import static java.net.HttpURLConnection.HTTP_BAD_GATEWAY;
import static java.net.HttpURLConnection.HTTP_OK;
import static java.util.Objects.nonNull;
import static no.unit.nva.search.RequestUtil.*;
import static no.unit.nva.search.SearchResourcesApiHandlerTest.*;
import static no.unit.nva.search.constants.ApplicationConstants.objectMapperWithEmpty;
import static no.unit.nva.testutils.RandomDataGenerator.objectMapper;
import static no.unit.nva.testutils.RandomDataGenerator.randomString;
import static nva.commons.core.ioutils.IoUtils.stringFromResources;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.collection.IsEmptyCollection.empty;
import static org.hamcrest.core.Is.is;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class SwsResourceHandlerTest {

    private SwsResourceHandler handler;
    private Context contextMock;
    private ByteArrayOutputStream outputStream;
    private SwsOpenSearchClient mockedSearchClient;

    @BeforeEach
    void setUp() {

        mockedSearchClient = mock(SwsOpenSearchClient.class);
        handler = new SwsResourceHandler(new Environment(), mockedSearchClient);
        contextMock = mock(Context.class);
        outputStream = new ByteArrayOutputStream();
    }

    @Test
    void shouldReturnSearchResultsWhenQueryIsSingleTerm() throws IOException {
        prepareRestHighLevelClientOkResponse();

        handler.handleRequest(getInputStream(), outputStream, contextMock);

        var gatewayResponse =
            GatewayResponse.<SearchResponseDto>of(outputStream);


        var actualBody =
            objectMapper.readValue(gatewayResponse.getBody(), new TypeReference<SearchResponseDto>() { });

        var expected = getSearchResourcesResponseFromFile(ROUNDTRIP_RESPONSE_JSON);

        assertNotNull(gatewayResponse.getHeaders());
        assertEquals(HTTP_OK, gatewayResponse.getStatusCode());
        assertThat(actualBody, is(equalTo(expected)));
    }

    @Test
    void shouldReturnSortedSearchResultsWhenSendingContributorId() throws IOException {
        prepareRestHighLevelClientOkResponse();
        handler.handleRequest(getInputStreamWithContributorId(), outputStream, contextMock);

        var gatewayResponse = GatewayResponse.<SearchResponseDto>of(outputStream);

        assertNotNull(gatewayResponse.getHeaders());
        assertEquals(HTTP_OK, gatewayResponse.getStatusCode());
    }

    @Test
    void shouldSearchResultsWhenSendingContributorIdAndBadResponseFromPreferencesApi()
        throws IOException {
        prepareRestHighLevelClientOkResponse();
        handler.handleRequest(getInputStreamWithContributorId(), outputStream, contextMock);

        var gatewayResponse = GatewayResponse.<SearchResponseDto>of(outputStream);

        assertNotNull(gatewayResponse.getHeaders());
        assertEquals(HTTP_OK, gatewayResponse.getStatusCode());
    }

    @Test
    void shouldNotReturnSortedSearchResultsWhenSendingMultipleContributorId()
        throws IOException {
        prepareRestHighLevelClientOkResponse();

        handler.handleRequest(getInputStreamWithMultipleContributorId(), outputStream, contextMock);

        var gatewayResponse = GatewayResponse.<SearchResponseDto>of(outputStream);

        assertNotNull(gatewayResponse.getHeaders());
        assertEquals(HTTP_OK, gatewayResponse.getStatusCode());
    }

    @Test
    @Disabled
    void shouldReturnAggregationAsPartOfResponseWhenDoingASearch() throws IOException {
        prepareRestHighLevelClientOkResponse();

        handler.handleRequest(getInputStream(), outputStream, contextMock);

        var gatewayResponse = GatewayResponse.<SearchResponseDto>of(outputStream);
        var actualBody =
            objectMapper.readValue(gatewayResponse.getBody(), new TypeReference<SearchResponseDto>() { });

        var expected = getSearchResourcesResponseFromFile(ROUNDTRIP_RESPONSE_JSON);

        assertNotNull(gatewayResponse.getHeaders());
        assertEquals(HTTP_OK, gatewayResponse.getStatusCode());
        assertThat(actualBody, is(equalTo(expected)));
        assertNotNull(actualBody.getAggregations());
        assertNotNull(actualBody.getAggregations().get("Bidragsyter"));
    }

    @Test
    void shouldReturnSearchResultsWithEmptyHitsWhenQueryResultIsEmpty() throws IOException {
        prepareRestHighLevelClientEmptyResponse();

        handler.handleRequest(getInputStream(), outputStream, mock(Context.class));

        var gatewayResponse = GatewayResponse.<SearchResponseDto>of(outputStream);
        var actualBody =
            objectMapper.readValue(gatewayResponse.getBody(), new TypeReference<SearchResponseDto>() { });

        assertNotNull(gatewayResponse.getHeaders());
        assertEquals(HTTP_OK, gatewayResponse.getStatusCode());
        assertThat(actualBody.getSize(), is(equalTo(0L)));
        assertThat(actualBody.getHits(), is(empty()));
        assertDoesNotThrow(() -> actualBody.getId().normalize());
    }

    @Test
    void shouldReturn200WhenSortOrderIsNotSpecified() throws IOException {
        prepareRestHighLevelClientEmptyResponseForSortOrder("desc");

        handler.handleRequest(getInputStream(), outputStream, mock(Context.class));

        var gatewayResponse = GatewayResponse.<SearchResponseDto>of(outputStream);
        var actualBody =
            objectMapper.readValue(gatewayResponse.getBody(), new TypeReference<SearchResponseDto>() { });

        assertNotNull(gatewayResponse.getHeaders());
        assertEquals(HTTP_OK, gatewayResponse.getStatusCode());
        assertThat(actualBody.getSize(), is(equalTo(0L)));
        assertThat(actualBody.getHits(), is(empty()));
        assertDoesNotThrow(() -> actualBody.getId().normalize());
    }

    @Test
    @Disabled
    void shouldReturn200WhenSortOrderIsDescInQueryParameters() throws IOException {
        prepareRestHighLevelClientEmptyResponseForSortOrder("desc");

        var queryParameters = Map.of(SEARCH_TERM_KEY, SAMPLE_SEARCH_TERM, SORTORDER_KEY, "desc");

        var inputStream = new HandlerRequestBuilder<Void>(objectMapperWithEmpty).withQueryParameters(queryParameters)
                              .withRequestContext(getRequestContext())
                              .build();

        handler.handleRequest(inputStream, outputStream, mock(Context.class));

        var gatewayResponse = GatewayResponse.<SearchResponseDto>of(outputStream);
        var actualBody =
            objectMapper.readValue(gatewayResponse.getBody(), new TypeReference<SearchResponseDto>() { });

        assertNotNull(gatewayResponse.getHeaders());
        assertEquals(HTTP_OK, gatewayResponse.getStatusCode());
        assertThat(actualBody.getSize(), is(equalTo(0L)));
        assertThat(actualBody.getHits(), is(empty()));
        assertDoesNotThrow(() -> actualBody.getId().normalize());
    }

    @Test
    @Disabled
    void shouldReturn200WhenSortOrderIsAscInQueryParameters() throws IOException {
        prepareRestHighLevelClientEmptyResponseForSortOrder("asc");

        var queryParameters = Map.of(SEARCH_TERM_KEY, SAMPLE_SEARCH_TERM, SORTORDER_KEY, "asc");

        var inputStream = new HandlerRequestBuilder<Void>(objectMapperWithEmpty).withQueryParameters(queryParameters)
                              .withRequestContext(getRequestContext())
                              .build();

        handler.handleRequest(inputStream, outputStream, mock(Context.class));

        var gatewayResponse = GatewayResponse.<SearchResponseDto>of(outputStream);
        var actualBody =
            objectMapper.readValue(gatewayResponse.getBody(), new TypeReference<SearchResponseDto>() { });

        assertNotNull(gatewayResponse.getHeaders());
        assertEquals(HTTP_OK, gatewayResponse.getStatusCode());
        assertThat(actualBody.getSize(), is(equalTo(0L)));
        assertThat(actualBody.getHits(), is(empty()));
        assertDoesNotThrow(() -> actualBody.getId().normalize());
    }

    @Test
    @Disabled
    void shouldReturnBadGatewayResponseWhenNoResponseFromService() throws IOException {
        prepareRestHighLevelClientEmptyResponse();

        handler.handleRequest(getInputStream(), outputStream, mock(Context.class));

        var gatewayResponse = GatewayResponse.<Problem>of(outputStream);


        assertNotNull(gatewayResponse.getHeaders());
        assertEquals(HTTP_BAD_GATEWAY, gatewayResponse.getStatusCode());
    }

    @ParameterizedTest(name = "should return application/json for accept header {0}")
    @MethodSource("acceptHeaderValuesProducingApplicationJsonProvider")
    void shouldProduceApplicationJsonWithGivenAcceptHeader(String acceptHeaderValue)
        throws IOException {
        prepareRestHighLevelClientOkResponse();
        var requestInput =
            nonNull(acceptHeaderValue) ? getRequestInputStreamAccepting(acceptHeaderValue) : getInputStream();
        handler.handleRequest(requestInput, outputStream, mock(Context.class));

        var gatewayResponse = GatewayResponse.<SearchResponseDto>of(outputStream);
        assertThat(gatewayResponse.getHeaders().get("Content-Type"), is(equalTo("application/json; charset=utf-8")));
    }

    private InputStream getInputStream() throws JsonProcessingException {
        return new HandlerRequestBuilder<Void>(objectMapperWithEmpty).withQueryParameters(
            Map.of(SEARCH_TERM_KEY, SAMPLE_SEARCH_TERM)).withRequestContext(getRequestContext()).build();
    }

    private InputStream getInputStreamWithContributorId() throws JsonProcessingException {
        return new HandlerRequestBuilder<Void>(objectMapperWithEmpty).withQueryParameters(
                Map.of(SEARCH_TERM_KEY, "entityDescription.contributors.identity" + ".id:12345&results=10&from=0"))
                   .withRequestContext(getRequestContext())
                   .withUserName(randomString())
                   .build();
    }

    private InputStream getInputStreamWithMultipleContributorId() throws JsonProcessingException {
        return
            new HandlerRequestBuilder<Void>(objectMapperWithEmpty)
                .withQueryParameters(
                    Map.of(SEARCH_TERM_KEY,
                           "(entityDescription.contributors.identity.id:12345)"
                           + "+AND+"
                           + "(entityDescription.contributors.identity.id:54321)"))
                .withRequestContext(getRequestContext())
                .withUserName(randomString())
                .build();
    }

    private InputStream getRequestInputStreamAccepting(String contentType) throws JsonProcessingException {
        return new HandlerRequestBuilder<Void>(objectMapperWithEmpty).withQueryParameters(
                Map.of(SEARCH_TERM_KEY, SAMPLE_SEARCH_TERM))
                   .withHeaders(Map.of("Accept", contentType))
                   .withRequestContext(getRequestContext())
                   .build();
    }

    private ObjectNode getRequestContext() {
        return objectMapperWithEmpty.convertValue(Map.of(PATH, SAMPLE_PATH, DOMAIN_NAME, SAMPLE_DOMAIN_NAME),
                                                  ObjectNode.class);
    }


    private void prepareRestHighLevelClientOkResponse() throws IOException {
        var jsonResponse = stringFromResources(Path.of(SAMPLE_OPENSEARCH_RESPONSE_WITH_AGGREGATION_JSON));
        var typeReference = new TypeReference<OpenSearchResponseDto>() { };
        var response = objectMapper.readValue(jsonResponse,typeReference);

        when(mockedSearchClient.doSearch(any()))
            .thenReturn(response.toSearchResponseDto(getSearchURI()));
    }


    private void prepareRestHighLevelClientEmptyResponse() throws IOException {
        var jsonResponse = stringFromResources(Path.of(EMPTY_OPENSEARCH_RESPONSE_JSON));
        var typeReference = new TypeReference<OpenSearchResponseDto>() { };
        var response = objectMapper.readValue(jsonResponse,typeReference);

        when(mockedSearchClient.doSearch(any()))
            .thenReturn(response.toSearchResponseDto(getSearchURI()));
    }

    private void prepareRestHighLevelClientEmptyResponseForSortOrder(String sortOrder)
        throws IOException {
        var jsonResponse = stringFromResources(Path.of(EMPTY_OPENSEARCH_RESPONSE_JSON));
        var typeReference = new TypeReference<OpenSearchResponseDto>() { };
        var response = objectMapper.readValue(jsonResponse,typeReference);

        when(mockedSearchClient.doSearch(any()))
            .thenReturn(response.toSearchResponseDto(getSearchURI()));
    }

    private SearchResponseDto getSearchResourcesResponseFromFile(String filename) throws JsonProcessingException {
        return objectMapperWithEmpty.readValue(stringFromResources(Path.of(filename)), SearchResponseDto.class);
    }

    private URI getSearchURI() {
        return URI.create("https://localhost/search?query=searchTerm");
    }

    public static Stream<String> acceptHeaderValuesProducingApplicationJsonProvider() {
        return Stream.of(null, "application/json");
    }

}