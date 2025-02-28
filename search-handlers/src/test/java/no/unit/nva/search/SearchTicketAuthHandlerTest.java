package no.unit.nva.search;

import static java.net.HttpURLConnection.HTTP_OK;
import static java.util.Objects.nonNull;
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
import static org.mockito.ArgumentMatchers.argThat;
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
import java.net.http.HttpClient;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Map;
import java.util.stream.Collectors;
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
  public static final URI TOP_LEVEL_CRISTIN_ID =
      URI.create("https://api.dev.nva.aws.unit" + ".no/cristin/organization/20754.0.0.0");
  private SearchTicketAuthHandler handler;
  private Context contextMock;
  private ByteArrayOutputStream outputStream;
  private TicketClient mockedSearchClient;

  @BeforeEach
  void setUp() {

    mockedSearchClient = mock(TicketClient.class);
    handler =
        new SearchTicketAuthHandler(
            new Environment(), mockedSearchClient, HttpClient.newHttpClient());
    contextMock = mock(Context.class);
    outputStream = new ByteArrayOutputStream();
  }

  @Test
  void shouldOnlyReturnPublicationsFromCuratorsOrganizationWhenQuerying()
      throws IOException, URISyntaxException {
    prepareRestHighLevelClientOkResponse();

    var input =
        getInputStreamWithAccessRight(
            randomUri(), new AccessRight[] {AccessRight.MANAGE_DOI}, new URI[] {});

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

    var input =
        getInputStreamWithAccessRight(
            randomUri(), new AccessRight[] {AccessRight.SUPPORT}, new URI[] {});
    handler.handleRequest(input, outputStream, contextMock);

    var gatewayResponse = FakeGatewayResponse.of(outputStream);

    assertNotNull(gatewayResponse.headers());
    assertEquals(HTTP_OK, gatewayResponse.statusCode());
  }

  @Test
  void shouldFilterOnUsersTopLevelOrgWhenNeitherViewingScopeOrQueryParamFilterIsInEffect()
      throws IOException {

    var jsonResponse =
        stringFromResources(Path.of(SAMPLE_OPENSEARCH_RESPONSE_WITH_AGGREGATION_JSON));
    var body = objectMapperWithEmpty.readValue(jsonResponse, SwsResponse.class);
    when(mockedSearchClient.doSearch(
            argThat(new TicketSearchQueryArgumentMatcher(TOP_LEVEL_CRISTIN_ID))))
        .thenReturn(body);

    var input =
        getInputStreamWithAccessRight(
            randomUri(),
            new AccessRight[] {
              AccessRight.SUPPORT, AccessRight.MANAGE_DOI, AccessRight.MANAGE_DEGREE
            },
            new URI[] {});

    handler.handleRequest(input, outputStream, contextMock);

    var gatewayResponse = FakeGatewayResponse.of(outputStream);

    assertThat(gatewayResponse.statusCode(), is(equalTo(HTTP_OK)));
  }

  @Test
  void shouldFilterOnViewingScopeOrgsIfPresentWhenQueryParamOrgFilterIsNotPresent()
      throws IOException {

    URI firstViewingScope = randomUri();
    URI secondViewingScope = randomUri();

    var jsonResponse =
        stringFromResources(Path.of(SAMPLE_OPENSEARCH_RESPONSE_WITH_AGGREGATION_JSON));
    var body = objectMapperWithEmpty.readValue(jsonResponse, SwsResponse.class);
    when(mockedSearchClient.doSearch(
            argThat(new TicketSearchQueryArgumentMatcher(firstViewingScope, secondViewingScope))))
        .thenReturn(body);

    var input =
        getInputStreamWithAccessRight(
            randomUri(),
            new AccessRight[] {
              AccessRight.SUPPORT, AccessRight.MANAGE_DOI, AccessRight.MANAGE_DEGREE
            },
            new URI[] {firstViewingScope, secondViewingScope});

    handler.handleRequest(input, outputStream, contextMock);

    var gatewayResponse = FakeGatewayResponse.of(outputStream);

    assertThat(gatewayResponse.statusCode(), is(equalTo(HTTP_OK)));
  }

  private void prepareRestHighLevelClientOkResponse() throws IOException {
    var jsonResponse =
        stringFromResources(Path.of(SAMPLE_OPENSEARCH_RESPONSE_WITH_AGGREGATION_JSON));
    var body = objectMapperWithEmpty.readValue(jsonResponse, SwsResponse.class);

    when(mockedSearchClient.doSearch(any())).thenReturn(body);
  }

  private InputStream getInputStreamWithAccessRight(
      URI customer, AccessRight[] accessRights, URI[] viewingScopes)
      throws JsonProcessingException {
    var personAffiliationId = "https://api.dev.nva.aws.unit.no/cristin/organization/20754.1.0.0";
    var personAffiliation = "custom:personAffiliation";
    var handlerRequestBuilder =
        new HandlerRequestBuilder<Void>(objectMapperWithEmpty)
            .withHeaders(Map.of(ACCEPT, "application/json"))
            .withRequestContext(getRequestContext())
            .withTopLevelCristinOrgId(TOP_LEVEL_CRISTIN_ID)
            .withAuthorizerClaim(personAffiliation, personAffiliationId)
            .withUserName(randomString())
            .withAuthorizerClaim(
                "custom:viewingScopeIncluded",
                Arrays.stream(viewingScopes).map(URI::toString).collect(Collectors.joining(",")))
            .withAuthorizerClaim("custom:viewingScopeExcluded", "null")
            .withCurrentCustomer(customer);
    if (nonNull(accessRights) && accessRights.length > 0) {
      handlerRequestBuilder.withAccessRights(customer, accessRights);
    }

    if (nonNull(viewingScopes) && viewingScopes.length > 0) {
      var viewingScopesIncluded =
          Arrays.stream(viewingScopes).map(URI::toString).collect(Collectors.toSet());
      handlerRequestBuilder.withAuthorizerClaim(
          "custom:viewingScopeIncluded", String.join(",", viewingScopesIncluded));
    }
    return handlerRequestBuilder.build();
  }

  private ObjectNode getRequestContext() {
    return objectMapperWithEmpty.convertValue(
        Map.of("path", SAMPLE_PATH, "domainName", SAMPLE_DOMAIN_NAME), ObjectNode.class);
  }
}
