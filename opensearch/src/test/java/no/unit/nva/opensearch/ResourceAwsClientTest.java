package no.unit.nva.opensearch;

import static no.unit.nva.indexing.testutils.MockedJwtProvider.setupMockedCachedJwtProvider;
import static no.unit.nva.opensearch.model.ResourceParameterKey.CATEGORY;
import static no.unit.nva.opensearch.model.ResourceParameterKey.FROM;
import static no.unit.nva.opensearch.model.ResourceParameterKey.SIZE;
import static no.unit.nva.opensearch.model.ResourceParameterKey.SORT;
import static nva.commons.core.ioutils.IoUtils.stringFromResources;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
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
import no.unit.nva.opensearch.model.common.OpenSearchQuery;
import nva.commons.apigateway.exceptions.ApiGatewayException;
import nva.commons.apigateway.exceptions.BadRequestException;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

class ResourceAwsClientTest {

    private ResourceAwsClient resourceAwsClient;

    public static final String SAMPLE_OPENSEARCH_RESPONSE_RESPONSE_EXPORT
        = "sample_opensearch_response.json";

    @BeforeEach
    public void setUp() throws IOException, InterruptedException {
        var httpClient = mock(HttpClient.class);
        var cachedJwtProvider = setupMockedCachedJwtProvider();
        resourceAwsClient = new ResourceAwsClient(cachedJwtProvider, httpClient);
        when(httpClient.send(any(), any()))
            .thenReturn(mockedHttpResponse(SAMPLE_OPENSEARCH_RESPONSE_RESPONSE_EXPORT));
    }

    @ParameterizedTest
    @MethodSource("uriProvider")
    void searchWithUriReturnsOpenSearchAwsResponse(URI uri) throws ApiGatewayException {
        var pagedSearchResourceDto =
            ResourceAwsQuery.builder()
                .fromQueryParameters(OpenSearchQuery.queryToMapEntries(uri))
                .withRequiredParameters(FROM, SIZE, SORT)
                .build()
                .doSearch(resourceAwsClient);

        assertNotNull(pagedSearchResourceDto);
    }

    @ParameterizedTest
    @MethodSource("uriSortingProvider")
    void searchUriWithSortingReturnsOpenSearchAwsResponse(URI uri) throws ApiGatewayException {
        var query =
            ResourceAwsQuery.builder()
                .fromQueryParameters(OpenSearchQuery.queryToMapEntries(uri))
                .withRequiredParameters(FROM, SIZE, SORT, CATEGORY)
                .build();

        var pagedSearchResourceDto = query.fetchAsPagedResponse(resourceAwsClient);
        assertNotNull(pagedSearchResourceDto.id());
        assertNotNull(pagedSearchResourceDto.context());
        assertTrue(pagedSearchResourceDto.id().getScheme().contains("https"));
    }

    @ParameterizedTest
    @MethodSource("uriInvalidProvider")
    void failToSearchUri(URI uri) {
        assertThrows(BadRequestException.class,
                     () -> ResourceAwsQuery.builder()
                               .fromQueryParameters(OpenSearchQuery.queryToMapEntries(uri))
                               .withRequiredParameters(FROM, SIZE)
                               .build()
                               .doSearch(resourceAwsClient));
    }

    static Stream<URI> uriSortingProvider() {
        return Stream.of(
            URI.create("https://example.com/?category=PhdThesis&sort=title&sortOrder=asc&sort=category&order"
                       + "=desc"),
            URI.create("https://example.com/?category=PhdThesis&sort=title&sortOrder=asc&sort=category"),
            URI.create("https://example.com/?category=PhdThesis&sort=title&sortOrder=asc&sort=category"),
            URI.create("https://example.com/?category=PhdThesis&size=10&from=0&sort=category"),
            URI.create("https://example.com/?category=PhdThesis&orderBy=contributor_name:asc,institution_name:desc"),
            URI.create("https://example.com/?category=PhdThesis&orderBy=institutionName:asc,"
                       + "modifiedDate:desc&searchAfter=1241234,23412"),
            URI.create("https://example.com/?category=PhdThesis&sort=unitId+asc&sort=contributor_name+desc"));
    }

    static Stream<URI> uriProvider() {
        return Stream.of(
            URI.create("https://example.com/?query=hello+world&lang=en&fields=category,title"),
            URI.create("https://example.com/?query=hello+world&lang=en&fields=category,title,werstfg"),
            URI.create("https://example.com/?title=hello+world&modified_before=2019-01-01"),
            URI.create("https://example.com/?contributor=hello+world&published_before=2020"),
            URI.create("https://example.com/?user=hello+world&lang=en&PUBLISHED_SINCE=2019"),
            URI.create("https://example.com/?user=hello+world&size=1&from=0"),
            URI.create("https://example.com/?query=hello+world&fields=all"));
    }

    static Stream<URI> uriInvalidProvider() {
        return Stream.of(
            URI.create("https://example.com/?categories=hello+world&lang=en"),
            URI.create("https://example.com/?tittles=hello+world&modified_before=2019-01-01"),
            URI.create("https://example.com/?conttributors=hello+world&published_before=2020"),
            URI.create("https://example.com/?category=PhdThesis&sort=beunited+asc"),
            URI.create("https://example.com/?useers=hello+world&lang=en"));
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