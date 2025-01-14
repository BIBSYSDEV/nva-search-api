package no.unit.nva.search.handlers;

import static java.net.HttpURLConnection.HTTP_OK;
import static no.unit.nva.search.model.constant.Defaults.objectMapperWithEmpty;
import static no.unit.nva.search.service.resource.ResourceParameter.SEARCH_ALL;
import static nva.commons.core.ioutils.IoUtils.stringFromResources;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.amazonaws.services.lambda.runtime.Context;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.nio.file.Path;
import java.util.Map;
import no.unit.nva.search.FakeGatewayResponse;
import no.unit.nva.search.model.records.SwsResponse;
import no.unit.nva.search.service.resource.ResourceClient;
import no.unit.nva.search.service.resource.response.ResourceSearchResponse;
import no.unit.nva.testutils.HandlerRequestBuilder;
import nva.commons.core.Environment;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class SearchResource20241201HandlerTest {
  public static final String SAMPLE_PATH = "search";
  public static final String SAMPLE_DOMAIN_NAME = "localhost";
  public static final String SAMPLE_SEARCH_TERM = "searchTerm";
  public static final String SAMPLE_OPENSEARCH_RESPONSE_WITH_AGGREGATION_JSON =
      "sample_opensearch_response.json";
  private SearchResourceHandler handler;
  private Context contextMock;
  private ByteArrayOutputStream outputStream;
  private ResourceClient mockedSearchClient;

  @BeforeEach
  void setUp() {

    mockedSearchClient = mock(ResourceClient.class);
    handler = new SearchResourceHandler(new Environment(), mockedSearchClient);
    contextMock = mock(Context.class);
    outputStream = new ByteArrayOutputStream();
  }

  @Test
  public void shouldReturnAResponseThatCanBeMappedToModelDto() throws IOException {
    prepareRestHighLevelClientOkResponse();
    handler.handleRequest(getInputStream(), outputStream, contextMock);
    var gatewayResponse = FakeGatewayResponse.of(outputStream);

    assertThat(gatewayResponse.statusCode(), is(equalTo(HTTP_OK)));

    var actualBody = gatewayResponse.body();
    var firstHitJson = actualBody.hits().getFirst();

    assertNotNull(firstHitJson);

    var firstHitDto = objectMapperWithEmpty.treeToValue(firstHitJson, ResourceSearchResponse.class);
    assertNotNull(firstHitDto);

    assertThat(
        firstHitDto.id(),
        is(
            equalTo(
                URI.create("http://localhost/publication/f367b260-c15e-4d0f-b197-e1dc0e9eb0e8"))));
  }

  private void prepareRestHighLevelClientOkResponse() throws IOException {
    var jsonResponse =
        stringFromResources(Path.of(SAMPLE_OPENSEARCH_RESPONSE_WITH_AGGREGATION_JSON));
    var body = objectMapperWithEmpty.readValue(jsonResponse, SwsResponse.class);

    when(mockedSearchClient.doSearch(any())).thenReturn(body);
  }

  private InputStream getInputStream() throws JsonProcessingException {
    return new HandlerRequestBuilder<Void>(objectMapperWithEmpty)
        .withQueryParameters(Map.of(SEARCH_ALL.name(), SAMPLE_SEARCH_TERM))
        .withRequestContext(getRequestContext())
        .build();
  }

  private ObjectNode getRequestContext() {
    return objectMapperWithEmpty.convertValue(
        Map.of("path", SAMPLE_PATH, "domainName", SAMPLE_DOMAIN_NAME), ObjectNode.class);
  }
}
