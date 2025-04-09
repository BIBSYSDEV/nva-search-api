package no.unit.nva.search;

import static java.net.HttpURLConnection.HTTP_FORBIDDEN;
import static java.net.HttpURLConnection.HTTP_INTERNAL_ERROR;
import static java.net.HttpURLConnection.HTTP_OK;
import static java.net.HttpURLConnection.HTTP_UNAUTHORIZED;
import static no.unit.nva.testutils.RandomDataGenerator.randomString;
import static no.unit.nva.testutils.RandomDataGenerator.randomUri;
import static nva.commons.apigateway.AccessRight.MANAGE_OWN_RESOURCES;
import static nva.commons.apigateway.ApiGatewayHandler.ALLOWED_ORIGIN_ENV;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.core.Is.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Collections;
import no.unit.nva.commons.json.JsonUtils;
import no.unit.nva.constants.Words;
import no.unit.nva.search.common.records.SwsResponse;
import no.unit.nva.search.common.records.SwsResponse.HitsInfo;
import no.unit.nva.search.common.records.SwsResponse.HitsInfo.TotalInfo;
import no.unit.nva.search.resource.ResourceClient;
import no.unit.nva.search.resource.ResourceParameter;
import no.unit.nva.search.testing.common.ResourceSearchQueryMatcher;
import no.unit.nva.search.testing.common.ResourceSearchQueryMatcher.TermsQueryBuilderExpectation;
import no.unit.nva.stubs.FakeContext;
import no.unit.nva.testutils.HandlerRequestBuilder;
import nva.commons.apigateway.GatewayResponse;
import nva.commons.core.Environment;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

public class UserBasedResourceSearchHandlerTest {
  private final ByteArrayOutputStream outputStream = new ByteArrayOutputStream();

  private UserBasedResourceSearchHandler handlerUnderTest;
  private ResourceClient resourceClient;

  @BeforeEach
  void beforeEach() {
    resourceClient = mock(ResourceClient.class);
    var environment = mock(Environment.class);
    when(environment.readEnv(ALLOWED_ORIGIN_ENV)).thenReturn("*");
    handlerUnderTest = new UserBasedResourceSearchHandler(environment, resourceClient);
  }

  @Test
  void shouldReturnInternalServerErrorOnUnexpectedException() throws IOException {

    doThrow(RuntimeException.class).when(resourceClient).doSearch(any(), eq(Words.RESOURCES));

    var request =
        new HandlerRequestBuilder<Void>(JsonUtils.dtoObjectMapper)
            .withUserName(randomString())
            .withAccessRights(randomUri(), MANAGE_OWN_RESOURCES)
            .withRequestContextValue("domainName", randomString())
            .build();

    handlerUnderTest.handleRequest(request, outputStream, new FakeContext());

    var response = GatewayResponse.fromOutputStream(outputStream, String.class);

    assertThat(response.getStatusCode(), is(equalTo(HTTP_INTERNAL_ERROR)));
  }

  @Test
  void shouldReturnUnauthorizedIfNoUserIsPresent() throws IOException {
    var request = new HandlerRequestBuilder<Void>(JsonUtils.dtoObjectMapper).build();

    handlerUnderTest.handleRequest(request, outputStream, new FakeContext());

    var response = GatewayResponse.fromOutputStream(outputStream, String.class);

    assertThat(response.getStatusCode(), is(equalTo(HTTP_UNAUTHORIZED)));
  }

  @Test
  void shouldReturnForbiddenIfUserCantManageOwnResources() throws IOException {
    var request =
        new HandlerRequestBuilder<Void>(JsonUtils.dtoObjectMapper)
            .withUserName(randomString())
            .build();

    handlerUnderTest.handleRequest(request, outputStream, new FakeContext());

    var response = GatewayResponse.fromOutputStream(outputStream, String.class);

    assertThat(response.getStatusCode(), is(equalTo(HTTP_FORBIDDEN)));
  }

  @Test
  void shouldQueryOpenSearchWithDefaultParametersIfNoQueryParametersIsPresent() throws IOException {
    var username = randomString();
    when(resourceClient.doSearch(any(), eq(Words.RESOURCES))).thenReturn(emptySwsResponse());
    var request =
        new HandlerRequestBuilder<Void>(JsonUtils.dtoObjectMapper)
            .withUserName(username)
            .withAccessRights(randomUri(), MANAGE_OWN_RESOURCES)
            .withRequestContextValue("domainName", randomString())
            .build();

    handlerUnderTest.handleRequest(request, outputStream, new FakeContext());

    var response = GatewayResponse.fromOutputStream(outputStream, String.class);

    assertThat(response.getStatusCode(), is(equalTo(HTTP_OK)));

    var matcher =
        new ResourceSearchQueryMatcher.Builder()
            .withPageParameter(ResourceParameter.FROM, "0")
            .withPageParameter(ResourceParameter.SIZE, "15")
            .withPageParameter(ResourceParameter.SORT, "relevance,identifier")
            .withSearchParameter(ResourceParameter.AGGREGATION, "none")
            .withNamedFilterQuery(
                "owner", new TermsQueryBuilderExpectation("resourceOwner.owner.keyword", username))
            .build();
    verify(resourceClient, Mockito.times(1)).doSearch(argThat(matcher), eq(Words.RESOURCES));
  }

  private static SwsResponse emptySwsResponse() {
    return new SwsResponse(
        100,
        false,
        null,
        new HitsInfo(new TotalInfo(100, null), 100d, Collections.emptyList()),
        null,
        null);
  }
}
