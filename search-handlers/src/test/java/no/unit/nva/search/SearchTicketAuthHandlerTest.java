package no.unit.nva.search;

import static java.net.HttpURLConnection.HTTP_OK;
import static no.unit.nva.auth.uriretriever.UriRetriever.ACCEPT;
import static no.unit.nva.constants.Defaults.objectMapperWithEmpty;
import static no.unit.nva.testutils.RandomDataGenerator.randomString;
import static no.unit.nva.testutils.RandomDataGenerator.randomUri;
import static nva.commons.core.ioutils.IoUtils.stringFromResources;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
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
import java.net.URISyntaxException;
import java.nio.file.Path;
import java.util.Map;
import no.unit.nva.search.common.FakeGatewayResponse;
import no.unit.nva.search.common.records.SwsResponse;
import no.unit.nva.search.ticket.TicketClient;
import no.unit.nva.testutils.HandlerRequestBuilder;
import nva.commons.apigateway.AccessRight;
import nva.commons.core.Environment;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class SearchTicketAuthHandlerTest {

  public static final String SAMPLE_PATH = "search";
  public static final String SAMPLE_DOMAIN_NAME = "localhost";
  public static final String SAMPLE_OPENSEARCH_RESPONSE_WITH_AGGREGATION_JSON =
      "sample_opensearch_response.json";
  private SearchTicketAuthHandler handler;
  private Context contextMock;
  private ByteArrayOutputStream outputStream;
  private TicketClient mockedSearchClient;

  @BeforeEach
  void setUp() {

    mockedSearchClient = mock(TicketClient.class);
    handler = new SearchTicketAuthHandler(new Environment(), mockedSearchClient);
    contextMock = mock(Context.class);
    outputStream = new ByteArrayOutputStream();
  }

  @Test
  void shouldOnlyReturnPublicationsFromCuratorsOrganizationWhenQuerying()
      throws IOException, URISyntaxException {
    prepareRestHighLevelClientOkResponse();

    var organization = new URI("https://api.dev.nva.aws.unit.no/cristin/organization/20754.0.0.0");
    var input = getInputStreamWithAccessRight(organization, AccessRight.MANAGE_DOI);

    handler.handleRequest(input, outputStream, contextMock);

    var gatewayResponse = FakeGatewayResponse.of(outputStream);
    var actualBody = gatewayResponse.body();

    assertNotNull(gatewayResponse.headers());
    assertEquals(HTTP_OK, gatewayResponse.statusCode());
    assertThat(actualBody.hits().size(), is(equalTo(2)));
  }

  @Test
  void shouldReturnOkWhenUserIsEditor() throws IOException {
    prepareRestHighLevelClientOkResponse();

    var input = getInputStreamWithAccessRight(randomUri(), AccessRight.SUPPORT);
    handler.handleRequest(input, outputStream, contextMock);

    var gatewayResponse = FakeGatewayResponse.of(outputStream);

    assertNotNull(gatewayResponse.headers());
    assertEquals(HTTP_OK, gatewayResponse.statusCode());
  }

  private void prepareRestHighLevelClientOkResponse() throws IOException {
    var jsonResponse =
        stringFromResources(Path.of(SAMPLE_OPENSEARCH_RESPONSE_WITH_AGGREGATION_JSON));
    var body = objectMapperWithEmpty.readValue(jsonResponse, SwsResponse.class);

    when(mockedSearchClient.doSearch(any())).thenReturn(body);
  }

  private InputStream getInputStreamWithAccessRight(URI organization, AccessRight accessRight)
      throws JsonProcessingException {
    var personAffiliationId = "https://api.dev.nva.aws.unit.no/cristin/organization/20754.1.0.0";
    var personAffiliation = "custom:personAffiliation";
    var topLevelOrgCristinId =
        URI.create("https://api.dev.nva.aws.unit.no/cristin/organization/20754.0.0.0");
    return new HandlerRequestBuilder<Void>(objectMapperWithEmpty)
        .withHeaders(Map.of(ACCEPT, "application/json"))
        .withRequestContext(getRequestContext())
        .withTopLevelCristinOrgId(topLevelOrgCristinId)
        .withAuthorizerClaim(personAffiliation, personAffiliationId)
        .withUserName(randomString())
        .withCurrentCustomer(organization)
        .withAccessRights(organization, accessRight)
        .build();
  }

  private ObjectNode getRequestContext() {
    return objectMapperWithEmpty.convertValue(
        Map.of("path", SAMPLE_PATH, "domainName", SAMPLE_DOMAIN_NAME), ObjectNode.class);
  }
}
