package no.unit.nva.search.resource;

import static java.net.HttpURLConnection.HTTP_BAD_GATEWAY;
import static java.net.HttpURLConnection.HTTP_OK;
import static no.unit.nva.testutils.RandomDataGenerator.randomString;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpClient.Version;
import java.net.http.HttpHeaders;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpResponse.BodyHandlers;
import java.util.Map;
import java.util.Optional;
import javax.net.ssl.SSLSession;
import nva.commons.logutils.LogUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class UserSettingsClientTest {

  private static UserSettingsClient userSettingsClient;
  private static HttpClient httpClient;

  @BeforeEach
  public void setUp() {
    httpClient = mock(HttpClient.class);
    userSettingsClient = new UserSettingsClient(httpClient);
  }

  @Test
  void shouldReturnPromotedPublicationsOnSuccess() throws IOException, InterruptedException {
    var contributorId = randomString();
    var value =
        FakeHttpResponse.create(
            """
            {
              "promotedPublications": [
                "https://api.dev.nva.aws.unit.no/publication/123",
                "https://api.dev.nva.aws.unit.no/publication/456"
              ]
            }
            """,
            HTTP_OK);
    when(httpClient.send(any(HttpRequest.class), eq(BodyHandlers.ofString()))).thenReturn(value);
    var promotedPublications = userSettingsClient.fetchPromotedPublications(contributorId);

    assertFalse(promotedPublications.isEmpty());
  }

  @Test
  void shouldReturnEmptyListOnFailureAndLogResponseThatFailed()
      throws IOException, InterruptedException {
    var contributorId = randomString();
    when(httpClient.send(any(HttpRequest.class), eq(BodyHandlers.ofString())))
        .thenReturn(FakeHttpResponse.create("{}", HTTP_BAD_GATEWAY));

    var logger = LogUtils.getTestingAppenderForRootLogger();
    var promotedPublications = userSettingsClient.fetchPromotedPublications(contributorId);

    assertTrue(
        logger
            .getMessages()
            .contains(
                String.format("Failed to fetch user settings for contributor %s", contributorId)));
    assertTrue(promotedPublications.isEmpty());
  }

  @Test
  void shouldNotSendAuthorizationHeaderWhenRequestingPromotedPublications()
      throws IOException, InterruptedException {
    var contributorId = randomString();
    when(httpClient.send(any(HttpRequest.class), eq(BodyHandlers.ofString())))
        .thenReturn(FakeHttpResponse.create("{}", HTTP_BAD_GATEWAY));

    userSettingsClient.fetchPromotedPublications(contributorId);

    var requestCaptor = ArgumentCaptor.forClass(HttpRequest.class);
    verify(httpClient).send(requestCaptor.capture(), eq(BodyHandlers.ofString()));
    assertFalse(requestCaptor.getValue().headers().map().containsKey("Authorization"));
  }

  private record FakeHttpResponse(String body, int statuCode) implements HttpResponse<String> {

    public static FakeHttpResponse create(String body, int statuCode) {
      return new FakeHttpResponse(body, statuCode);
    }

    @Override
    public int statusCode() {
      return statuCode;
    }

    @Override
    public HttpRequest request() {
      return null;
    }

    @Override
    public Optional<HttpResponse<String>> previousResponse() {
      return Optional.empty();
    }

    @Override
    public HttpHeaders headers() {
      return HttpHeaders.of(Map.of(), (s1, s2) -> true);
    }

    @Override
    public Optional<SSLSession> sslSession() {
      return Optional.empty();
    }

    @Override
    public URI uri() {
      return null;
    }

    @Override
    public Version version() {
      return null;
    }
  }
}
