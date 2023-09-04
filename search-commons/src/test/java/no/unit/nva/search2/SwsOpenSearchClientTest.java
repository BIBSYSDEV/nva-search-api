package no.unit.nva.search2;


import com.fasterxml.jackson.core.JsonProcessingException;
import no.unit.nva.auth.uriretriever.RawContentRetriever;
import no.unit.nva.search.models.UsernamePasswordWrapper;
import nva.commons.secrets.SecretsReader;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
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

import static no.unit.nva.search2.SwsOpenSearchClient.prepareWithSecretReader;
import static nva.commons.core.ioutils.IoUtils.stringFromResources;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class SwsOpenSearchClientTest {

    private SwsOpenSearchClient swsOpenSearchClient;
    private static final String MEDIA_TYPE = "application/json";
    private static final String NO_HITS_RESPONSE_JSON = "no_hits_response.json";
    public static final String SAMPLE_OPENSEARCH_RESPONSE_RESPONSE_EXPORT
        = "sample_opensearch_response_export.json";

    private RawContentRetriever contentRetriever;

    @BeforeEach
    public void setUp() throws JsonProcessingException {
        contentRetriever = mock(RawContentRetriever.class);
        var mockedResponse = MockedHttpResponse();

        // Use the mocked response in your test code
        when(contentRetriever.fetchResponse(any(URI.class), anyString()))
            .thenReturn(Optional.of(mockedResponse));

        swsOpenSearchClient = new SwsOpenSearchClient(contentRetriever, MEDIA_TYPE);
    }

    @NotNull
    private HttpResponse<String> MockedHttpResponse() {
        return new HttpResponse<String>() {
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
        var secretsReaderMock = mock(SecretsReader.class);
        var testCredentials = new UsernamePasswordWrapper("user", "password");
        when(secretsReaderMock.fetchClassSecret(anyString(), eq(UsernamePasswordWrapper.class)))
            .thenReturn(testCredentials);
        var swsOpenSearchClient = prepareWithSecretReader(secretsReaderMock);
        assertNotNull(swsOpenSearchClient);
    }

    @ParameterizedTest
    @MethodSource("uriProvider")
    void searchSingleTermReturnsPagedResponse(URI uri) throws IOException {
        var searchResponseDto =
            swsOpenSearchClient.doSearch(uri).body();
        assertNotNull(searchResponseDto);
    }

    @ParameterizedTest
    @MethodSource("uriProvider")
    void searchSingleTermReturnsResponse(URI uri) throws IOException {
        var searchResponseDto =
            swsOpenSearchClient.doSearch(uri);

        assertNotNull(searchResponseDto);
    }
    static Stream<URI> uriProvider() {
        return Stream.of(
            URI.create("https://example.com/_search?q=name:hello+world&lang=en"),
            URI.create("https://example.com/_search?q=name:hello+world&lang=en"),
            URI.create("https://example.com/_search?q=name:hello+world&lang=en"));
    }
}