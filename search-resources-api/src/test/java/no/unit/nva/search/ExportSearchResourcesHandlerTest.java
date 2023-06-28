package no.unit.nva.search;

import com.amazonaws.services.lambda.runtime.Context;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.node.ObjectNode;
import no.unit.nva.indexing.testutils.FakeSearchResponse;
import no.unit.nva.indexing.testutils.csv.CsvUtil;
import no.unit.nva.testutils.HandlerRequestBuilder;
import nva.commons.apigateway.GatewayResponse;
import nva.commons.core.Environment;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.opensearch.action.search.SearchResponse;
import org.opensearch.client.RestHighLevelClient;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Map;

import static java.net.HttpURLConnection.HTTP_OK;
import static no.unit.nva.indexing.testutils.SearchResponseUtil.getSearchResponseFromJson;
import static no.unit.nva.indexing.testutils.TestSetup.setupMockedCachedJwtProvider;
import static no.unit.nva.search.RequestUtil.DOMAIN_NAME;
import static no.unit.nva.search.RequestUtil.PATH;
import static no.unit.nva.search.RequestUtil.SEARCH_TERM_KEY;
import static no.unit.nva.search.constants.ApplicationConstants.objectMapperWithEmpty;
import static no.unit.nva.testutils.RandomDataGenerator.randomString;
import static no.unit.nva.testutils.RandomDataGenerator.randomUri;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ExportSearchResourcesHandlerTest {

    public static final String SAMPLE_SEARCH_TERM = "searchTerm";
    public static final String SAMPLE_PATH = "search";
    public static final String SAMPLE_DOMAIN_NAME = "localhost";
    public static final String COMMA_DELIMITER = ", ";

    private RestHighLevelClient restHighLevelClientMock;
    private ExportSearchResourcesHandler handler;
    private Context contextMock;
    private ByteArrayOutputStream outputStream;

    @BeforeEach
    void init() {
        restHighLevelClientMock = mock(RestHighLevelClient.class);
        var cachedJwtProvider = setupMockedCachedJwtProvider();
        var restHighLevelClientWrapper = new RestHighLevelClientWrapper(restHighLevelClientMock);
        var searchClient = new SearchClient(restHighLevelClientWrapper, cachedJwtProvider);
        handler = new ExportSearchResourcesHandler(new Environment(), searchClient);
        contextMock = mock(Context.class);
        outputStream = new ByteArrayOutputStream();
    }

    @Test
    void shouldReturnSearchResultsWhenQueryIsSingleTerm() throws IOException {
        var expected = List.of(csvWithFullDate(), csvWithYearOnly());
        prepareRestHighLevelClientOkResponse(expected);
        handler.handleRequest(getInputStream(), outputStream, contextMock);
        var response = GatewayResponse.fromOutputStream(outputStream, String.class);

        var body = response.getBody().trim();
        var actual = CsvUtil.toExportCsv(body);

        assertNotNull(response.getHeaders());
        assertEquals(HTTP_OK, response.getStatusCode());
        assertThat(actual, is(equalTo(expected)));
    }


    private void prepareRestHighLevelClientOkResponse(List<ExportCsv> exportCsvs) throws IOException {
        var json = FakeSearchResponse.generateSearchResponseString(exportCsvs);
        var searchResponse = createSearchResponseWithHits(json);
        when(restHighLevelClientMock.search(any(), any())).thenReturn(searchResponse);
    }

    private SearchResponse createSearchResponseWithHits(String json) throws IOException {
        return getSearchResponseFromJson(json);
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
}