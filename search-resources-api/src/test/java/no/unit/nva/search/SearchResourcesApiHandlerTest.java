package no.unit.nva.search;

import static java.net.HttpURLConnection.HTTP_BAD_GATEWAY;
import static java.net.HttpURLConnection.HTTP_OK;
import static java.util.Objects.nonNull;
import static no.unit.nva.indexing.testutils.SearchResponseUtil.getSearchResponseFromJson;
import static no.unit.nva.indexing.testutils.TestSetup.setupMockedCachedJwtProvider;
import static no.unit.nva.search.RequestUtil.DOMAIN_NAME;
import static no.unit.nva.search.RequestUtil.PATH;
import static no.unit.nva.search.RequestUtil.SEARCH_TERM_KEY;
import static no.unit.nva.search.RequestUtil.SORTORDER_KEY;
import static no.unit.nva.search.constants.ApplicationConstants.objectMapperWithEmpty;
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
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import com.amazonaws.services.lambda.runtime.Context;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;
import no.unit.nva.indexing.testutils.FakeSearchResponse;
import no.unit.nva.search.models.SearchResponseDto;
import no.unit.nva.search.utils.SortKeyHttpRequestMatcher;
import no.unit.nva.testutils.HandlerRequestBuilder;
import nva.commons.apigateway.GatewayResponse;
import nva.commons.core.Environment;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.opensearch.action.search.SearchResponse;
import org.opensearch.client.RestHighLevelClient;
import org.zalando.problem.Problem;

public class SearchResourcesApiHandlerTest {

    private static final char UTF8_BOM = '\ufeff';
    private static final String CRLF = "\r\n";
    public static final String SAMPLE_SEARCH_TERM = "searchTerm";
    public static final String SAMPLE_OPENSEARCH_RESPONSE_WITH_AGGREGATION_JSON
        = "sample_opensearch_response.json";
    public static final String EMPTY_OPENSEARCH_RESPONSE_JSON = "empty_opensearch_response.json";
    public static final String ROUNDTRIP_RESPONSE_JSON = "roundtripResponse.json";
    public static final String SAMPLE_PATH = "search";
    public static final String SAMPLE_DOMAIN_NAME = "localhost";
    private static final CharSequence COMMA_DELIMITER = ",";

    private RestHighLevelClient restHighLevelClientMock;
    private SearchResourcesApiHandler handler;
    private Context contextMock;
    private ByteArrayOutputStream outputStream;

    public static Stream<String> acceptHeaderValuesProducingApplicationJsonProvider() {
        return Stream.of(null, "application/json");
    }

    public static Stream<String> acceptHeaderValuesProducingTextCsvProvider() {
        return Stream.of("text/*", "text/csv");
    }

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

    @ParameterizedTest(name = "should return application/json for accept header {0}")
    @MethodSource("acceptHeaderValuesProducingApplicationJsonProvider")
    void shouldProduceApplicationJsonWithGivenAcceptHeader(String acceptHeaderValue) throws IOException {
        prepareRestHighLevelClientOkResponse();
        var requestInput =
            nonNull(acceptHeaderValue) ? getRequestInputStreamAccepting(acceptHeaderValue) : getInputStream();
        handler.handleRequest(requestInput, outputStream, mock(Context.class));

        GatewayResponse<String> gatewayResponse = GatewayResponse.fromOutputStream(outputStream, String.class);
        assertThat(gatewayResponse.getHeaders().get("Content-Type"), is(equalTo("application/json; charset=utf-8")));
    }

    @ParameterizedTest(name = "should return text/csv for accept header {0}")
    @MethodSource("acceptHeaderValuesProducingTextCsvProvider")
    void shouldReturnTextCsvWithGivenAcceptHeader(String acceptHeaderValue) throws IOException {
        prepareRestHighLevelClientOkResponse(List.of(csvWithFullDate(), csvWithYearOnly()));
        handler.handleRequest(getRequestInputStreamAccepting(acceptHeaderValue), outputStream, mock(Context.class));

        GatewayResponse<String> gatewayResponse = GatewayResponse.fromOutputStream(outputStream, String.class);
        assertThat(gatewayResponse.getHeaders().get("Content-Type"), is(equalTo("text/csv; charset=utf-8")));
    }

    @Test
    void textCsvShouldContainCorrectFormatting() throws IOException {
        var csvLines = List.of(csvWithFullDate(), csvWithYearOnly());
        prepareRestHighLevelClientOkResponse(csvLines);
        handler.handleRequest(getRequestInputStreamAccepting("text/csv"), outputStream, mock(Context.class));

        GatewayResponse<String> gatewayResponse = GatewayResponse.fromOutputStream(outputStream, String.class);

        assertCsvGeneratedCorrectly(gatewayResponse.getBody(), csvLines);
    }

    private ExportCsv csvWithYearOnly() {
        var id = randomUri().toString();
        var title = randomString();
        var type = "AcademicArticle";
        var contributors = List.of(randomString(), randomString(), randomString());
        var date = "2022";

        var exportCsv = new ExportCsv();
        exportCsv.setId(id);
        exportCsv.setMainTitle(title);
        exportCsv.setPublicationInstance(type);
        exportCsv.setPublicationDate(date);
        exportCsv.setContributors(String.join(COMMA_DELIMITER, contributors));
        return exportCsv;
    }

    private static ExportCsv csvWithFullDate() {
        var id = randomUri().toString();
        var title = randomString();
        var type = "AcademicArticle";
        var contributors = List.of(randomString(), randomString(), randomString());
        var date = "2022-01-22";

        var exportCsv = new ExportCsv();
        exportCsv.setId(id);
        exportCsv.setMainTitle(title);
        exportCsv.setPublicationInstance(type);
        exportCsv.setPublicationDate(date);
        exportCsv.setContributors(String.join(COMMA_DELIMITER, contributors));
        return exportCsv;
    }

    private void assertCsvGeneratedCorrectly(String body, List<ExportCsv> expectedlines) {
        // first character must be UTF-8 BOM
        assertThat("CSV must contain UTF-8 BOM at the beginning!", body.charAt(0), is(equalTo(UTF8_BOM)));

        var lines = body.substring(1).split(CRLF);
        var expectedNumberOfLines = expectedlines.size() + 1;
        assertThat("CSV must use \\r\\n as line separator and contain a column header row!",
                   lines.length, is(equalTo(expectedNumberOfLines)));

        assertThat("Should have column header names in first line!",
                   lines[0], is(equalTo("\"url\";\"title\";\"publicationDate\";\"type\";\"contributors\"")));

        var idx = 0;
        for (var line : expectedlines) {
            assertThat(lines[idx + 1], is(equalTo(expectedLine(line))));
            idx++;
        }
    }

    private String expectedLine(ExportCsv line) {
        return "\"" + line.getId() + "\";"
               + "\"" + line.getMainTitle() + "\";"
               + "\"" + line.getPublicationDate() + "\";"
               + "\"" + line.getPublicationInstance() + "\";"
               + "\"" + line.getContributors() + "\"";
    }

    private InputStream getInputStream() throws JsonProcessingException {
        return new HandlerRequestBuilder<Void>(objectMapperWithEmpty)
                   .withQueryParameters(Map.of(SEARCH_TERM_KEY, SAMPLE_SEARCH_TERM))
                   .withRequestContext(getRequestContext())
                   .build();
    }

    private InputStream getRequestInputStreamAccepting(String contentType) throws JsonProcessingException {
        return new HandlerRequestBuilder<Void>(objectMapperWithEmpty)
                   .withQueryParameters(Map.of(SEARCH_TERM_KEY, SAMPLE_SEARCH_TERM))
                   .withHeaders(Map.of("Accept", contentType))
                   .withRequestContext(getRequestContext())
                   .build();
    }

    private ObjectNode getRequestContext() {
        return objectMapperWithEmpty.convertValue(Map.of(PATH, SAMPLE_PATH, DOMAIN_NAME, SAMPLE_DOMAIN_NAME),
                                                  ObjectNode.class);
    }

    private void prepareRestHighLevelClientOkResponse(List<ExportCsv> exportCsvs) throws IOException {
        var json = FakeSearchResponse.generateSearchResponseString(exportCsvs);
        var searchResponse = createSearchResponseWithHits(json);
        when(restHighLevelClientMock.search(any(), any())).thenReturn(searchResponse);
    }

    private SearchResponse createSearchResponseWithHits(String json) throws IOException {
        return getSearchResponseFromJson(json);
    }

    private void prepareRestHighLevelClientOkResponse() throws IOException {
        SearchResponse searchResponse =
            createSearchResponseWithHitsFromFile(SAMPLE_OPENSEARCH_RESPONSE_WITH_AGGREGATION_JSON);

        when(restHighLevelClientMock.search(any(), any())).thenReturn(searchResponse);
    }

    private void prepareRestHighLevelClientEmptyResponse() throws IOException {
        SearchResponse searchResponse = createSearchResponseWithHitsFromFile(EMPTY_OPENSEARCH_RESPONSE_JSON);

        when(restHighLevelClientMock.search(any(), any())).thenReturn(searchResponse);
    }

    private void prepareRestHighLevelClientEmptyResponseForSortOrder(String sortOrder) throws IOException {
        SearchResponse searchResponse = createSearchResponseWithHitsFromFile(EMPTY_OPENSEARCH_RESPONSE_JSON);
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

    private SearchResponse createSearchResponseWithHitsFromFile(String responseJsonFile) throws IOException {
        String jsonResponse = stringFromResources(Path.of(responseJsonFile));
        return getSearchResponseFromJson(jsonResponse);
    }
}
