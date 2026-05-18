package no.unit.nva.search;

import static java.net.HttpURLConnection.HTTP_OK;
import static java.net.HttpURLConnection.HTTP_UNAUTHORIZED;
import static no.unit.nva.auth.uriretriever.UriRetriever.ACCEPT;
import static no.unit.nva.constants.Defaults.objectMapperWithEmpty;
import static no.unit.nva.testutils.RandomDataGenerator.randomString;
import static no.unit.nva.testutils.RandomDataGenerator.randomUri;
import static nva.commons.core.ioutils.IoUtils.stringFromResources;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasEntry;
import static org.hamcrest.Matchers.hasKey;
import static org.hamcrest.core.Is.is;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.amazonaws.services.lambda.runtime.Context;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import no.unit.nva.constants.Words;
import no.unit.nva.search.common.records.SwsResponse;
import no.unit.nva.search.resource.ResourceClient;
import no.unit.nva.search.testing.common.FakeGatewayResponse;
import no.unit.nva.testutils.HandlerRequestBuilder;
import nva.commons.apigateway.AccessRight;
import nva.commons.core.Environment;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class SearchResourceAuthHandlerTest {

  public static final String SAMPLE_PATH = "search";
  public static final String SAMPLE_DOMAIN_NAME = "localhost";
  public static final String SAMPLE_OPENSEARCH_RESPONSE_WITH_AGGREGATION_JSON =
      "sample_opensearch_response.json";
  private SearchResourceAuthHandler handler;
  private Context contextMock;
  private ByteArrayOutputStream outputStream;
  private ResourceClient mockedSearchClient;

  @BeforeEach
  void setUp() {

    mockedSearchClient = mock(ResourceClient.class);
    handler = new SearchResourceAuthHandler(mockedSearchClient, new Environment());
    contextMock = mock(Context.class);
    outputStream = new ByteArrayOutputStream();
  }

  @Test
  void shouldOnlyReturnPublicationsFromCuratorsOrganizationWhenQuerying()
      throws IOException, URISyntaxException {
    prepareRestHighLevelClientOkResponse();

    var customer =
        new URI("https://api.dev.nva.aws.unit.no/customer/f54c8aa9-073a-46a1-8f7c-dde66c853934");
    var curatorOrganization =
        new URI("https://api.dev.nva.aws.unit.no/cristin/organization/184.0.0.0");

    handler.handleRequest(
        getInputStreamWithAccessRight(
            customer, curatorOrganization, AccessRight.MANAGE_RESOURCES_ALL),
        outputStream,
        contextMock);

    var gatewayResponse = FakeGatewayResponse.of(outputStream);
    var actualBody = gatewayResponse.body();

    assertNotNull(gatewayResponse.headers());
    assertEquals(HTTP_OK, gatewayResponse.statusCode());
    assertThat(actualBody.hits().size(), is(equalTo(2)));
  }

  @Test
  void shouldReturnOkWhenUserIsEditor() throws IOException {
    prepareRestHighLevelClientOkResponse();

    var input =
        getInputStreamWithAccessRight(randomUri(), randomUri(), AccessRight.MANAGE_RESOURCES_ALL);
    handler.handleRequest(input, outputStream, contextMock);

    var gatewayResponse = FakeGatewayResponse.of(outputStream);

    assertNotNull(gatewayResponse.headers());
    assertEquals(HTTP_OK, gatewayResponse.statusCode());
  }

  @Test
  void shouldEmitPaginationHeadersWhenRequestingBibtex() throws IOException {
    prepareRestHighLevelClientOkResponse();

    handler.handleRequest(
        getBibtexInputStreamWithAccessRight(
            randomUri(), randomUri(), AccessRight.MANAGE_RESOURCES_ALL),
        outputStream,
        contextMock);

    var headers = rawHeaders(outputStream);
    assertThat(headers, hasEntry("X-Total-Count", "2"));
    assertThat(headers, hasEntry("Access-Control-Expose-Headers", "Link, X-Total-Count"));
    assertThat(headers, hasKey("Link"));
    assertThat(headers.get("Link"), containsString("rel=\"first\""));
  }

  @Test
  void shouldReturnUnauthorizedWhenUserIsMissingAccessRight() throws IOException {
    prepareRestHighLevelClientOkResponse();

    var input = getInputStreamWithAccessRight(randomUri(), randomUri(), AccessRight.SUPPORT);
    handler.handleRequest(input, outputStream, contextMock);

    var gatewayResponse = FakeGatewayResponse.of(outputStream);

    assertNotNull(gatewayResponse.headers());
    assertEquals(HTTP_UNAUTHORIZED, gatewayResponse.statusCode());
  }

  private void prepareRestHighLevelClientOkResponse() throws IOException {
    var jsonResponse =
        stringFromResources(Path.of(SAMPLE_OPENSEARCH_RESPONSE_WITH_AGGREGATION_JSON));
    var body = objectMapperWithEmpty.readValue(jsonResponse, SwsResponse.class);

    when(mockedSearchClient.doSearch(any(), eq(Words.RESOURCES))).thenReturn(body);
  }

  private InputStream getInputStreamWithAccessRight(
      URI currentCustomer, URI topLevelCristinOrgId, AccessRight accessRight)
      throws JsonProcessingException {
    return new HandlerRequestBuilder<Void>(objectMapperWithEmpty)
        .withHeaders(Map.of(ACCEPT, "application/json"))
        .withRequestContext(getRequestContext())
        .withUserName(randomString())
        .withCurrentCustomer(currentCustomer)
        .withTopLevelCristinOrgId(topLevelCristinOrgId)
        .withAccessRights(currentCustomer, accessRight)
        .withHeaders(Map.of("Authorization", "Bearer " + randomString()))
        .build();
  }

  private InputStream getBibtexInputStreamWithAccessRight(
      URI currentCustomer, URI topLevelCristinOrgId, AccessRight accessRight)
      throws JsonProcessingException {
    return new HandlerRequestBuilder<Void>(objectMapperWithEmpty)
        .withMultiValueQueryParameters(Map.of("size", List.of("1")))
        .withRequestContext(getRequestContext())
        .withUserName(randomString())
        .withCurrentCustomer(currentCustomer)
        .withTopLevelCristinOrgId(topLevelCristinOrgId)
        .withAccessRights(currentCustomer, accessRight)
        .withHeaders(Map.of(ACCEPT, "text/x-bibtex", "Authorization", "Bearer " + randomString()))
        .build();
  }

  @SuppressWarnings("unchecked")
  private static Map<String, String> rawHeaders(ByteArrayOutputStream outputStream)
      throws IOException {
    var typeRef = new TypeReference<Map<String, Object>>() {};
    var response =
        objectMapperWithEmpty.readValue(outputStream.toString(StandardCharsets.UTF_8), typeRef);
    return (Map<String, String>) response.get("headers");
  }

  private ObjectNode getRequestContext() {
    return objectMapperWithEmpty.convertValue(
        Map.of("path", SAMPLE_PATH, "domainName", SAMPLE_DOMAIN_NAME), ObjectNode.class);
  }
}
