package no.unit.nva.search2;

import static no.unit.nva.indexing.testutils.MockedJwtProvider.setupMockedCachedJwtProvider;
import static no.unit.nva.search2.common.Query.queryToMapEntries;
import static no.unit.nva.search2.enums.ImportCandidateParameter.CATEGORY;
import static no.unit.nva.search2.enums.ImportCandidateParameter.FROM;
import static no.unit.nva.search2.enums.ImportCandidateParameter.SIZE;
import static no.unit.nva.search2.enums.ImportCandidateParameter.SORT;
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
import nva.commons.apigateway.exceptions.ApiGatewayException;
import nva.commons.apigateway.exceptions.BadRequestException;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

class ImportCandidateClientTest {

    private ImportCandidateClient importCandidateClient;

    public static final String SAMPLE_OPENSEARCH_RESPONSE_RESPONSE_EXPORT
        = "sample_opensearch_response.json";

    @BeforeEach
    public void setUp() throws IOException, InterruptedException {
        var httpClient = mock(HttpClient.class);
        var cachedJwtProvider = setupMockedCachedJwtProvider();
        importCandidateClient = new ImportCandidateClient(cachedJwtProvider, httpClient);
        when(httpClient.send(any(), any()))
            .thenReturn(mockedHttpResponse(SAMPLE_OPENSEARCH_RESPONSE_RESPONSE_EXPORT));
    }

    @ParameterizedTest
    @MethodSource("uriProvider")
    void searchWithUriReturnsOpenSearchAwsResponse(URI uri) throws ApiGatewayException {
        var pagedSearchResourceDto =
            ImportCandidateQuery.builder()
                .fromQueryParameters(queryToMapEntries(uri))
                .withRequiredParameters(FROM, SIZE, SORT)
                .build()
                .doSearch(importCandidateClient);

        assertNotNull(pagedSearchResourceDto);
    }

    @ParameterizedTest
    @MethodSource("uriProvider")
    void searchWithUriReturnsCSVResponse(URI uri) throws ApiGatewayException {

        var csvResult = ImportCandidateQuery.builder()
            .fromQueryParameters(queryToMapEntries(uri))
            .withRequiredParameters(FROM, SIZE, SORT)
            .withMediaType("text/csv")
            .build()
            .doSearch(importCandidateClient);
        assertNotNull(csvResult);
    }

    @ParameterizedTest
    @MethodSource("uriSortingProvider")
    void searchUriWithSortingReturnsOpenSearchAwsResponse(URI uri) throws ApiGatewayException {
        var query =
            ImportCandidateQuery.builder()
                .fromQueryParameters(queryToMapEntries(uri))
                .withRequiredParameters(FROM, SIZE, SORT, CATEGORY)
                .build();

        var response = importCandidateClient.doSearch(query);
        var pagedResponse = query.toPagedResponse(response);
        assertNotNull(pagedResponse.id());
        assertNotNull(pagedResponse.context());
        assertTrue(pagedResponse.id().getScheme().contains("https"));
    }

    @ParameterizedTest
    @MethodSource("uriInvalidProvider")
    void failToSearchUri(URI uri) {
        assertThrows(BadRequestException.class,
                     () -> ImportCandidateQuery.builder()
                         .fromQueryParameters(queryToMapEntries(uri))
                         .withRequiredParameters(FROM, SIZE)
                         .build()
                         .doSearch(importCandidateClient));
    }

    static Stream<URI> uriSortingProvider() {
        return Stream.of(
            URI.create("https://example.com/?category=PhdThesis&sort=title&sortOrder=asc&sort=category&order"
                       + "=desc"),
            URI.create("https://example.com/?category=PhdThesis&sort=title&sortOrder=asc&sort=category"),
            URI.create("https://example.com/?category=PhdThesis&sort=title&sortOrder=asc&sort=category"),
            URI.create("https://example.com/?category=PhdThesis&size=10&from=0&sort=category"),
            URI.create("https://example.com/?category=PhdThesis&orderBy=UNIT_ID:asc,title:desc"),
            URI.create("https://example.com/?category=PhdThesis&orderBy=title:asc,"
                       + "modifiedDate:desc&searchAfter=1241234,23412"),
            URI.create("https://example.com/?category=PhdThesis&sort=unitId+asc&sort=category+desc"));
    }

    static Stream<URI> uriProvider() {
        return Stream.of(
            URI.create("https://example.com/?query=hello+world&fields=category,title"),
            URI.create("https://example.com/?query=hello+world&fields=category,title,werstfg&ID_NOT=123"),
            URI.create("https://example.com/?title=http://hello+world&published_before=2019"),
            URI.create("https://example.com/?owner_should="
                       + "https://api.dev.nva.aws.unit.no/cristin/person/1136254+"
                       + "https://api.dev.nva.aws.unit.no/cristin/person/1135555"),
            URI.create("https://example.com/?owner_not=hello+world&PUBLISHED_SINCE=2019"),
            URI.create("https://example.com/?owner_should=hello+world&size=1&from=0"),
            URI.create("https://example.com/?PUBLISHED_BEFORE=1872&PUBLISHED_SINCE=9460"),
            URI.create("https://example.com/"),
            URI.create("https://example.com/?query=hello+world&fields=all"));
    }

    static Stream<URI> uriInvalidProvider() {
        return Stream.of(
            URI.create("https://example.com/?categories=hello+world"),
            URI.create("https://example.com/?tittles=hello+world&modified_before=2019-01"),
            URI.create("https://example.com/?conttributors=hello+world&published_before=2020-01-01"),
            URI.create("https://example.com/?category=PhdThesis&sort=beunited+asc"),
            URI.create("https://example.com/?funding=NFR,296896"),
            URI.create("https://example.com/?useers=hello+world"));
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