package no.unit.nva.search2;

import static no.unit.nva.indexing.testutils.MockedJwtProvider.setupMockedCachedJwtProvider;
import static no.unit.nva.search2.model.OpenSearchQuery.queryToMapEntries;
import static no.unit.nva.search2.model.ParameterKeyResource.FROM;
import static no.unit.nva.search2.model.ParameterKeyResource.SIZE;
import static no.unit.nva.search2.model.ParameterKeyResource.SORT;
import static nva.commons.core.ioutils.IoUtils.stringFromResources;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpHeaders;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Path;
import java.util.Collections;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Stream;
import javax.net.ssl.SSLSession;
import nva.commons.apigateway.exceptions.ApiGatewayException;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

class UserSettingsClientTest {

    private UserSettingsClient userSettingsClient;
    private static final Logger logger = LoggerFactory.getLogger(UserSettingsClientTest.class);
    public static final String SAMPLE_USERSETTINGS_RESPONSE
        = "user_settings.json";

    @BeforeEach
    public void setUp() throws IOException, InterruptedException {
        var httpClient = mock(HttpClient.class);
        var cachedJwtProvider = setupMockedCachedJwtProvider();
        userSettingsClient = new UserSettingsClient(cachedJwtProvider, httpClient);
        when(httpClient.send(any(), any()))
            .thenReturn(mockedHttpResponse(SAMPLE_USERSETTINGS_RESPONSE));
    }

    @ParameterizedTest
    @MethodSource("uriProvider")
    void searchWithUriReturnsOpenSearchAwsResponse(URI uri) throws ApiGatewayException {
        var resourceAwsQuery =
            ResourceAwsQuery.builder()
                .fromQueryParameters(queryToMapEntries(uri))
                .withRequiredParameters(FROM, SIZE, SORT)
                .build();
            var result = userSettingsClient.doSearch(resourceAwsQuery).promotedPublications();
        logger.info(result.toString());
        assertNotNull(result);
    }


    static Stream<URI> uriProvider() {
        return Stream.of(
            URI.create("https://example.com/?title=http://hello+world&modified_before=2019-01-01"),
            URI.create("https://example.com/?contributor=hello+:+world&published_before=2020-01-01"),
            URI.create("https://example.com/"),
            URI.create("https://example.com/?category=PhdThesis&sort=title&sortOrder=asc&sort=category"),
            URI.create("https://example.com/?category=PhdThesis&sort=title&sortOrder=asc&sort=category"),
            URI.create("https://example.com/?category=PhdThesis&size=10&from=0&sort=category"),
            URI.create("https://example.com/?category=PhdThesis&orderBy=UNIT_ID:asc,title:desc"),
            URI.create("https://example.com/?query=hello+world&fields=all"));
    }


    @NotNull
    public static HttpResponse<Object> mockedHttpResponse(String filename) {
        return new HttpResponse<>() {
            @Override
            public int statusCode() {
                return 200;
            }

            @Override
            public HttpRequest request() {
                return null;
            }

            @Override
            public Optional<HttpResponse<Object>> previousResponse() {
                return Optional.empty();
            }

            @Override
            public HttpHeaders headers() {
                return HttpHeaders.of(Map.of("Content-Type", Collections.singletonList("application/json")),
                                      (s, s2) -> true);
            }

            @Override
            public String body() {
                return stringFromResources(Path.of(filename));
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
            public HttpClient.Version version() {
                return null;
            }
        };
    }
}