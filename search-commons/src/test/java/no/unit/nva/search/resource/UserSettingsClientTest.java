package no.unit.nva.search.resource;

import static no.unit.nva.common.EntrySetTools.queryToMapEntries;
import static no.unit.nva.common.MockedHttpResponse.mockedFutureFailed;
import static no.unit.nva.common.MockedHttpResponse.mockedFutureHttpResponse;
import static no.unit.nva.indexing.testutils.MockedJwtProvider.setupMockedCachedJwtProvider;
import static no.unit.nva.search.resource.ResourceParameter.FROM;
import static no.unit.nva.search.resource.ResourceParameter.SIZE;

import static nva.commons.core.StringUtils.EMPTY_STRING;
import static nva.commons.core.attempt.Try.attempt;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import no.unit.nva.search.common.records.UserSettings;

import nva.commons.apigateway.exceptions.ApiGatewayException;
import nva.commons.core.attempt.Failure;
import nva.commons.core.attempt.FunctionWithException;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.net.http.HttpClient;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Stream;

class UserSettingsClientTest {

  public static final String SAMPLE_USER_SETTINGS_RESPONSE = "user_settings.json";
  private static final Logger logger = LoggerFactory.getLogger(UserSettingsClientTest.class);
  private static UserSettingsClient userSettingsClient;

  @BeforeAll
  public static void setUp() {
    var mochedHttpClient = mock(HttpClient.class);
    var cachedJwtProvider = setupMockedCachedJwtProvider();
    userSettingsClient = new UserSettingsClient(mochedHttpClient, cachedJwtProvider);
    final var path = Path.of(SAMPLE_USER_SETTINGS_RESPONSE);

    when(mochedHttpClient.sendAsync(any(), any()))
        .thenReturn(mockedFutureHttpResponse(EMPTY_STRING))
        .thenReturn(mockedFutureFailed())
        .thenReturn(mockedFutureHttpResponse(path))
        .thenReturn(mockedFutureHttpResponse(path));
  }

  static Stream<URI> uriProvider() {
    return Stream.of(
        URI.create(
            "https://example.com/?contributor=http://hello.worl.test.orgd&modified_before=2019-01-01"),
        URI.create(
            "https://example.com/?contributor=https://api.dev.nva.aws.unit.no/cristin/person/1269057"),
        URI.create(
            "https://example.com/?contributor=https%3A%2F%2Fapi.dev.nva.aws.unit"
                + ".no%2Fcristin%2Fperson%2F1269057&orderBy=UNIT_ID:asc,title:desc"),
        URI.create(
            "https://example.com/?contributor=https://api.dev.nva.aws.unit.no/cristin/person/1269051"));
  }

  @ParameterizedTest
  @MethodSource("uriProvider")
  void searchWithUriReturnsOpenSearchAwsResponse(URI uri) throws ApiGatewayException {
    var resourceAwsQuery =
        ResourceSearchQuery.builder()
            .fromTestQueryParameters(queryToMapEntries(uri))
            .withRequiredParameters(FROM, SIZE)
            .build();
    var promotedPublications =
        attempt(() -> userSettingsClient.doSearch(resourceAwsQuery))
            .map(UserSettings::promotedPublications)
            .orElse(logExceptionAndContinue());
    assertNotNull(promotedPublications);
  }

  private FunctionWithException<Failure<List<String>>, List<String>, RuntimeException>
      logExceptionAndContinue() {
    return (e) -> {
      if (e.isFailure()) {
        logger.error(e.getException().getMessage());
      }
      return List.of();
    };
  }
}
