package no.unit.nva.search;

import static no.unit.nva.auth.uriretriever.UriRetriever.ACCEPT;
import static no.unit.nva.search.model.constant.Defaults.objectMapperWithEmpty;
import static no.unit.nva.search.model.constant.Words.COMMA;
import static no.unit.nva.search.model.constant.Words.CONTRIBUTOR;
import static no.unit.nva.search.service.resource.ResourceParameter.SEARCH_ALL;
import static no.unit.nva.testutils.RandomDataGenerator.randomString;
import static no.unit.nva.testutils.RandomDataGenerator.randomUri;

import static nva.commons.core.ioutils.IoUtils.stringFromResources;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.collection.IsEmptyCollection.empty;
import static org.hamcrest.core.Is.is;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import static java.net.HttpURLConnection.HTTP_OK;
import static java.util.Objects.nonNull;

import com.amazonaws.services.lambda.runtime.Context;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.node.ObjectNode;

import no.unit.nva.indexing.testutils.FakeSearchResponse;
import no.unit.nva.search.model.constant.Words;
import no.unit.nva.search.model.csv.ExportCsv;
import no.unit.nva.search.model.records.SwsResponse;
import no.unit.nva.search.service.resource.ResourceClient;
import no.unit.nva.testutils.HandlerRequestBuilder;

import nva.commons.apigateway.GatewayResponse;
import nva.commons.core.Environment;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

class SearchResourceHandlerTest {

    public static final String SAMPLE_PATH = "search";
    public static final String SAMPLE_DOMAIN_NAME = "localhost";
    public static final String SAMPLE_SEARCH_TERM = "searchTerm";
    public static final String SAMPLE_OPENSEARCH_RESPONSE_WITH_AGGREGATION_JSON =
            "sample_opensearch_response.json";
    public static final String EMPTY_OPENSEARCH_RESPONSE_JSON = "empty_opensearch_response.json";
    private SearchResourceHandler handler;
    private Context contextMock;
    private ByteArrayOutputStream outputStream;
    private ResourceClient mockedSearchClient;

    private static ExportCsv csvWithFullDate() {
        var id = randomUri().toString();
        var title = randomString();
        var type = "AcademicArticle";
        var contributors = List.of(randomString(), randomString(), randomString());
        var date = "2022-01-22";

        return new ExportCsv()
                .withId(id)
                .withMainTitle(title)
                .withPublicationInstance(type)
                .withPublicationDate(date)
                .withContributors(String.join(COMMA, contributors));
    }

    public static Stream<String> acceptHeaderValuesProducingTextCsvProvider() {
        return Stream.of("text/*", Words.TEXT_CSV);
    }

    public static Stream<String> acceptHeaderValuesProducingApplicationJsonProvider() {
        return Stream.of(null, "application/json", "application/json; charset=utf-8");
    }

    @BeforeEach
    void setUp() {

        mockedSearchClient = mock(ResourceClient.class);
        handler = new SearchResourceHandler(new Environment(), mockedSearchClient);
        contextMock = mock(Context.class);
        outputStream = new ByteArrayOutputStream();
    }

    @ParameterizedTest(name = "should return text/csv for accept header {0}")
    @MethodSource("acceptHeaderValuesProducingTextCsvProvider")
    void shouldReturnTextCsvWithGivenAcceptHeader(String acceptHeaderValue) throws IOException {
        prepareRestHighLevelClientOkResponse(List.of(csvWithFullDate(), csvWithYearOnly()));
        handler.handleRequest(
                getRequestInputStreamAccepting(acceptHeaderValue),
                outputStream,
                mock(Context.class));

        GatewayResponse<String> gatewayResponse =
                GatewayResponse.fromOutputStream(outputStream, String.class);
        assertThat(
                gatewayResponse.getHeaders().get("Content-Type"),
                is(equalTo("text/csv; charset=utf-8")));
    }

    private ExportCsv csvWithYearOnly() {
        var id = randomUri().toString();
        var title = randomString();
        var type = "AcademicArticle";
        var contributors = List.of(randomString(), randomString(), randomString());
        var date = "2022";

        return new ExportCsv()
                .withId(id)
                .withMainTitle(title)
                .withPublicationInstance(type)
                .withPublicationDate(date)
                .withContributors(String.join(COMMA, contributors));
    }

    @Test
    void shouldReturnSortedSearchResultsWhenSendingContributorId() throws IOException {
        prepareRestHighLevelClientOkResponse();
        handler.handleRequest(getInputStreamWithContributorId(), outputStream, contextMock);

        var gatewayResponse = FakeGatewayResponse.of(outputStream);

        assertNotNull(gatewayResponse.headers());
        assertEquals(HTTP_OK, gatewayResponse.statusCode());
    }

    @Test
    void shouldNotReturnSortedSearchResultsWhenSendingMultipleContributorId() throws IOException {
        prepareRestHighLevelClientOkResponse();

        handler.handleRequest(getInputStreamWithMultipleContributorId(), outputStream, contextMock);

        var gatewayResponse = FakeGatewayResponse.of(outputStream);

        assertNotNull(gatewayResponse.headers());
        assertEquals(HTTP_OK, gatewayResponse.statusCode());
    }

    @Test
    void shouldReturnSearchResultsWithEmptyHitsWhenQueryResultIsEmpty() throws IOException {
        prepareRestHighLevelClientEmptyResponse();

        handler.handleRequest(getInputStream(), outputStream, mock(Context.class));

        var gatewayResponse = FakeGatewayResponse.of(outputStream);
        var actualBody = gatewayResponse.body();

        assertNotNull(gatewayResponse.headers());
        assertEquals(HTTP_OK, gatewayResponse.statusCode());
        assertThat(actualBody.totalHits(), is(equalTo(0)));
        assertThat(actualBody.hits(), is(empty()));
        assertDoesNotThrow(() -> gatewayResponse.body().id().normalize());
    }

    @Test
    void shouldReturn200WhenSortOrderIsNotSpecified() throws IOException {
        prepareRestHighLevelClientEmptyResponseForSortOrder();

        handler.handleRequest(getInputStream(), outputStream, mock(Context.class));

        var gatewayResponse = FakeGatewayResponse.of(outputStream);
        var actualBody = gatewayResponse.body();

        assertNotNull(gatewayResponse.headers());
        assertEquals(HTTP_OK, gatewayResponse.statusCode());
        assertThat(actualBody.totalHits(), is(equalTo(0)));
        assertThat(actualBody.hits(), is(empty()));
        assertDoesNotThrow(() -> gatewayResponse.body().id().normalize());
    }

    @ParameterizedTest(name = "Should return application/json for accept header {0}")
    @MethodSource("acceptHeaderValuesProducingApplicationJsonProvider")
    void shouldProduceWithHeader(String acceptHeaderValue) throws IOException {
        prepareRestHighLevelClientOkResponse();
        var requestInput =
                nonNull(acceptHeaderValue)
                        ? getRequestInputStreamAccepting(acceptHeaderValue)
                        : getInputStream();
        handler.handleRequest(requestInput, outputStream, mock(Context.class));

        var gatewayResponse = FakeGatewayResponse.of(outputStream);
        assertThat(
                gatewayResponse.headers().get("Content-Type"),
                is(equalTo("application/json; charset=utf-8")));
    }

    private InputStream getInputStream() throws JsonProcessingException {
        return new HandlerRequestBuilder<Void>(objectMapperWithEmpty)
                .withQueryParameters(Map.of(SEARCH_ALL.name(), SAMPLE_SEARCH_TERM))
                .withRequestContext(getRequestContext())
                .build();
    }

    private InputStream getInputStreamWithContributorId() throws JsonProcessingException {
        return new HandlerRequestBuilder<Void>(objectMapperWithEmpty)
                .withQueryParameters(
                        Map.of(
                                SEARCH_ALL.name(),
                                "entityDescription.contributors.identity.id:12345",
                                "results",
                                "10",
                                "from",
                                "0"))
                .withHeaders(Map.of(ACCEPT, "application/json"))
                .withRequestContext(getRequestContext())
                .withUserName(randomString())
                .build();
    }

    private InputStream getInputStreamWithMultipleContributorId() throws JsonProcessingException {
        return new HandlerRequestBuilder<Void>(objectMapperWithEmpty)
                .withMultiValueQueryParameters(Map.of(CONTRIBUTOR, List.of("12345", "54321")))
                .withRequestContext(getRequestContext())
                .withHeaders(Map.of(ACCEPT, "application/json"))
                .withUserName(randomString())
                .build();
    }

    private InputStream getRequestInputStreamAccepting(String contentType)
            throws JsonProcessingException {
        return new HandlerRequestBuilder<Void>(objectMapperWithEmpty)
                .withQueryParameters(Map.of(SEARCH_ALL.asCamelCase(), SAMPLE_SEARCH_TERM))
                .withHeaders(Map.of(ACCEPT, contentType))
                .withRequestContext(getRequestContext())
                .build();
    }

    private ObjectNode getRequestContext() {
        return objectMapperWithEmpty.convertValue(
                Map.of("path", SAMPLE_PATH, "domainName", SAMPLE_DOMAIN_NAME), ObjectNode.class);
    }

    private void prepareRestHighLevelClientOkResponse(List<ExportCsv> exportCsvs)
            throws IOException {
        var jsonResponse = FakeSearchResponse.generateSearchResponseString(exportCsvs, null);
        var body = objectMapperWithEmpty.readValue(jsonResponse, SwsResponse.class);

        when(mockedSearchClient.doSearch(any())).thenReturn(body);
        //        var searchResponse = createSearchResponseWithHits(json);
        //        when(restHighLevelClientMock.search(any(), any())).thenReturn(searchResponse);
    }

    private void prepareRestHighLevelClientOkResponse() throws IOException {
        var jsonResponse =
                stringFromResources(Path.of(SAMPLE_OPENSEARCH_RESPONSE_WITH_AGGREGATION_JSON));
        var body = objectMapperWithEmpty.readValue(jsonResponse, SwsResponse.class);

        when(mockedSearchClient.doSearch(any())).thenReturn(body);
    }

    private void prepareRestHighLevelClientEmptyResponse() throws IOException {
        var jsonResponse = stringFromResources(Path.of(EMPTY_OPENSEARCH_RESPONSE_JSON));
        var body = objectMapperWithEmpty.readValue(jsonResponse, SwsResponse.class);

        when(mockedSearchClient.doSearch(any())).thenReturn(body);
    }

    private void prepareRestHighLevelClientEmptyResponseForSortOrder() throws IOException {
        var jsonResponse = stringFromResources(Path.of(EMPTY_OPENSEARCH_RESPONSE_JSON));
        var body = objectMapperWithEmpty.readValue(jsonResponse, SwsResponse.class);

        when(mockedSearchClient.doSearch(any())).thenReturn(body);
    }
}
