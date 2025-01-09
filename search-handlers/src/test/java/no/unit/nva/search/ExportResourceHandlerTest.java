package no.unit.nva.search;

import static no.unit.nva.constants.Defaults.objectMapperWithEmpty;
import static no.unit.nva.constants.Words.COMMA;
import static no.unit.nva.search.resource.ResourceParameter.SEARCH_ALL;
import static no.unit.nva.testutils.RandomDataGenerator.randomString;
import static no.unit.nva.testutils.RandomDataGenerator.randomUri;
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
import no.unit.nva.search.common.csv.ExportCsv;
import no.unit.nva.search.common.records.SwsResponse;
import no.unit.nva.search.resource.ResourceClient;
import no.unit.nva.search.scroll.ScrollClient;
import no.unit.nva.testutils.HandlerRequestBuilder;
import nva.commons.apigateway.RequestInfo;
import nva.commons.apigateway.exceptions.BadRequestException;
import org.apache.commons.lang3.StringUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class ExportResourceHandlerTest {
  private static final String SAMPLE_PATH = "search";
  private static final String SAMPLE_DOMAIN_NAME = "localhost";
  private ResourceClient mockedResourceClient;
  private ScrollClient mockedScrollClient;
  private ExportResourceHandler handler;

  private static SwsResponse csvToSwsResponse(ExportCsv csv, String scrollId)
      throws JsonProcessingException {
    var jsonResponse = FakeSearchResponse.generateSearchResponseString(List.of(csv), scrollId);
    return objectMapperWithEmpty.readValue(jsonResponse, SwsResponse.class);
  }

  private static ExportCsv csvWithFullDate(String title) {
    var id = randomUri().toString();
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

  @BeforeEach
  void setUp() {
    mockedResourceClient = mock(ResourceClient.class);
    mockedScrollClient = mock(ScrollClient.class);
    handler = new ExportResourceHandler(mockedResourceClient, mockedScrollClient, null, null);
  }

  @Test
  void shouldReturnCsvWithTitleField() throws IOException, BadRequestException {
    var expectedTitle1 = randomString();
    var expectedTitle2 = randomString();
    var expectedTitle3 = randomString();
    prepareRestHighLevelClientOkResponse(
        csvWithFullDate(expectedTitle1),
        csvWithFullDate(expectedTitle2),
        csvWithFullDate(expectedTitle3));

    var s3data =
        handler.processS3Input(
            null, RequestInfo.fromRequest(getRequestInputStreamAccepting()), null);

    assertThat(StringUtils.countMatches(s3data, expectedTitle1), is(1));
    assertThat(StringUtils.countMatches(s3data, expectedTitle2), is(1));
    assertThat(StringUtils.countMatches(s3data, expectedTitle3), is(1));
  }

  private void prepareRestHighLevelClientOkResponse(
      ExportCsv initialSearchResult, ExportCsv scroll1SearchResult, ExportCsv scroll2SearchResult)
      throws IOException {

    when(mockedResourceClient.doSearch(any()))
        .thenReturn(csvToSwsResponse(initialSearchResult, "scrollId1"));

    when(mockedScrollClient.doSearch(any()))
        .thenReturn(csvToSwsResponse(scroll1SearchResult, "scrollId2"))
        .thenReturn(csvToSwsResponse(scroll2SearchResult, null));
  }

  private InputStream getRequestInputStreamAccepting() throws JsonProcessingException {
    return new HandlerRequestBuilder<Void>(objectMapperWithEmpty)
        .withQueryParameters(Map.of(SEARCH_ALL.asCamelCase(), "*"))
        .withRequestContext(getRequestContext())
        .build();
  }

  private ObjectNode getRequestContext() {
    return objectMapperWithEmpty.convertValue(
        Map.of("path", SAMPLE_PATH, "domainName", SAMPLE_DOMAIN_NAME), ObjectNode.class);
  }
}
