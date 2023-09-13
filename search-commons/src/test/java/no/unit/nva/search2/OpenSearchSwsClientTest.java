package no.unit.nva.search2;


import no.unit.nva.search2.common.OpenSearchSwsClient;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import javax.net.ssl.SSLSession;
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

import static no.unit.nva.indexing.testutils.MockedJwtProvider.setupMockedCachedJwtProvider;
import static nva.commons.core.ioutils.IoUtils.stringFromResources;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class OpenSearchSwsClientTest {

    private OpenSearchSwsClient openSearchSwsClient;
    private static final String MEDIA_TYPE = "application/json";
    public static final String SAMPLE_OPENSEARCH_RESPONSE_RESPONSE_EXPORT
        = "sample_opensearch_response.json";

    @BeforeEach
    public void setUp() throws IOException, InterruptedException {
        var httpClient = mock(HttpClient.class);
        var mockedResponse = mockedHttpResponse();
        var cachedJwtProvider = setupMockedCachedJwtProvider();

        when(httpClient.send(any(), any())).thenReturn(mockedResponse);

        openSearchSwsClient = new OpenSearchSwsClient(cachedJwtProvider, httpClient);
    }

    @NotNull
    private HttpResponse<Object> mockedHttpResponse() {
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
                return stringFromResources(Path.of(SAMPLE_OPENSEARCH_RESPONSE_RESPONSE_EXPORT));
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

    @ParameterizedTest
    @MethodSource("uriProvider")
    void searchSingleTermReturnsOpenSearchSwsResponse(URI uri) {
        var searchResponseDto = openSearchSwsClient.doSearch(uri, MEDIA_TYPE);
        assertNotNull(searchResponseDto);
    }

    static Stream<URI> uriProvider() {
        return Stream.of(
            URI.create("https://example.com/_search?q=name:hello+world&lang=en"),
            URI.create("https://example.com/_search?q=contributor:hello+world&lang=en"),
            URI.create("https://example.com/_search?q=name:hello+world&lang=en"));
    }
}