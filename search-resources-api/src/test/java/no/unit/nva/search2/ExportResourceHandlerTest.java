package no.unit.nva.search2;

import static no.unit.nva.search2.common.constant.Defaults.objectMapperWithEmpty;
import static no.unit.nva.search2.common.constant.Words.COMMA;
import static no.unit.nva.search2.resource.ResourceParameter.SEARCH_ALL;
import static no.unit.nva.testutils.RandomDataGenerator.randomString;
import static no.unit.nva.testutils.RandomDataGenerator.randomUri;
import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Map;
import no.unit.nva.indexing.testutils.FakeSearchResponse;
import no.unit.nva.search.ExportCsv;
import no.unit.nva.search2.common.records.SwsResponse;
import no.unit.nva.search2.resource.ResourceClient;
import no.unit.nva.testutils.HandlerRequestBuilder;
import nva.commons.apigateway.RequestInfo;
import nva.commons.apigateway.exceptions.BadRequestException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class ExportResourceHandlerTest {
    public static final String SAMPLE_PATH = "search";
    public static final String SAMPLE_DOMAIN_NAME = "localhost";
    private ResourceClient mockedSearchClient;
    private ExportResourceHandler handler;

    @BeforeEach
    void setUp() {

        mockedSearchClient = mock(ResourceClient.class);
        handler = new ExportResourceHandler(mockedSearchClient, null, null);
    }
    @Test
    void shouldReturnCsvWithTitleField() throws IOException, BadRequestException {
        var expectedTitle = randomString();
        prepareRestHighLevelClientOkResponse(List.of(csvWithFullDate(expectedTitle)));

        var s3data = handler.processS3Input(null, RequestInfo.fromRequest(getRequestInputStreamAccepting()), null);

        assertThat(s3data, containsString(expectedTitle));
    }

    private void prepareRestHighLevelClientOkResponse(List<ExportCsv> exportCsvs) throws IOException {
        var jsonResponse = FakeSearchResponse.generateSearchResponseString(exportCsvs);
        var body = objectMapperWithEmpty.readValue(jsonResponse, SwsResponse.class);

        when(mockedSearchClient.doSearch(any()))
            .thenReturn(body);
    }

    private static ExportCsv csvWithFullDate(String title) {
        var id = randomUri().toString();
        var type = "AcademicArticle";
        var contributors = List.of(randomString(), randomString(), randomString());
        var date = "2022-01-22";

        var exportCsv = new ExportCsv();
        exportCsv.setId(id);
        exportCsv.setMainTitle(title);
        exportCsv.setPublicationInstance(type);
        exportCsv.setPublicationDate(date);
        exportCsv.setContributors(String.join(COMMA, contributors));
        return exportCsv;
    }

    private InputStream getRequestInputStreamAccepting() throws JsonProcessingException {
        return new HandlerRequestBuilder<Void>(objectMapperWithEmpty).withQueryParameters(
                Map.of(SEARCH_ALL.asCamelCase(), "*"))
                   .withRequestContext(getRequestContext())
                   .build();
    }

    private ObjectNode getRequestContext() {
        return objectMapperWithEmpty.convertValue(
            Map.of("path", SAMPLE_PATH, "domainName", SAMPLE_DOMAIN_NAME), ObjectNode.class);
    }
}