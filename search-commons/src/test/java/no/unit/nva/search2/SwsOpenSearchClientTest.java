package no.unit.nva.search2;


import no.unit.nva.auth.uriretriever.RawContentRetriever;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import javax.net.ssl.SSLSession;
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

import static nva.commons.core.ioutils.IoUtils.stringFromResources;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class SwsOpenSearchClientTest {

    private SwsOpenSearchClient swsOpenSearchClient;
    private static final String MEDIA_TYPE = "application/json";
    private static final String NO_HITS_RESPONSE_JSON = "no_hits_response.json";
    public static final String SAMPLE_OPENSEARCH_RESPONSE_RESPONSE_EXPORT
        = "sample_opensearch_response.json";


    @BeforeEach
    public void setUp() {
        var contentRetriever = mock(RawContentRetriever.class);
        var mockedResponse = mockedHttpResponse();

        // Use the mocked response in your test code
        when(contentRetriever.fetchResponse(any(URI.class), anyString()))
            .thenReturn(Optional.of(mockedResponse));

        swsOpenSearchClient = new SwsOpenSearchClient(contentRetriever, MEDIA_TYPE);
    }

    @NotNull
    private HttpResponse<String> mockedHttpResponse() {
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
            public Optional<HttpResponse<String>> previousResponse() {
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

    @Test
    void constructorWithSecretsReaderDefinedShouldCreateInstance() {
        var secretsReaderMock = mock(RawContentRetriever.class);
        var swsOpenSearchClient =  new SwsOpenSearchClient(secretsReaderMock, MEDIA_TYPE);
        assertNotNull(swsOpenSearchClient);
    }

    @ParameterizedTest
    @MethodSource("uriProvider")
    void searchSingleTermReturnsPagedResponse(URI uri) {
        var searchResponseDto =
            swsOpenSearchClient.doSearch(uri);
        assertNotNull(searchResponseDto);
    }

    @ParameterizedTest
    @MethodSource("uriProvider")
    void searchSingleTermReturnsResponse(URI uri) {
        var searchResponseDto =
            swsOpenSearchClient.doSearch(uri);

        assertNotNull(searchResponseDto);
    }

    static Stream<URI> uriProvider() {
        return Stream.of(
            URI.create("https://example.com/_search?q=name:hello+world&lang=en"),
            URI.create("https://example.com/_search?q=contributor:hello+world&lang=en"),
            URI.create("https://example.com/_search?q=name:hello+world&lang=en"));
    }
}