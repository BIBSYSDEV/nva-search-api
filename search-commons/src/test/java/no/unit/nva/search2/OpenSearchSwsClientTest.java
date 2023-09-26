package no.unit.nva.search2;

import static java.util.Objects.nonNull;
import static no.unit.nva.indexing.testutils.MockedJwtProvider.setupMockedCachedJwtProvider;
import static no.unit.nva.search2.model.OpenSearchQuery.queryToMapEntries;
import static no.unit.nva.search2.model.ResourceParameterKey.CATEGORY;
import static no.unit.nva.search2.model.ResourceParameterKey.FROM;
import static no.unit.nva.search2.model.ResourceParameterKey.SIZE;
import static no.unit.nva.search2.model.ResourceParameterKey.SORT;
import static nva.commons.core.ioutils.IoUtils.stringFromResources;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
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
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.net.ssl.SSLSession;
import no.unit.nva.search2.model.OpenSearchQuery;
import nva.commons.apigateway.exceptions.ApiGatewayException;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

class OpenSearchSwsClientTest {

    public static final String SAMPLE_OPENSEARCH_RESPONSE_RESPONSE_EXPORT
        = "sample_opensearch_response.json";
    private OpenSearchSwsClient openSearchSwsClient;

    public final Logger logger = LoggerFactory.getLogger(OpenSearchSwsClientTest.class);

    @BeforeEach
    public void setUp() throws IOException, InterruptedException {

        var httpClient = mock(HttpClient.class);
        var cachedJwtProvider = setupMockedCachedJwtProvider();
        openSearchSwsClient = new OpenSearchSwsClient(cachedJwtProvider, httpClient);
        when(httpClient.send(any(), any()))
            .thenReturn(mockedHttpResponse(SAMPLE_OPENSEARCH_RESPONSE_RESPONSE_EXPORT));
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

    @ParameterizedTest
    @MethodSource("uriProvider")
    void whenResultsContainNextIdValidateOffset(URI uri) throws ApiGatewayException {

        var pagedSearchResourceDto =
            ResourceSwsQuery.Builder
                .queryBuilder()
                .fromQueryParameters(OpenSearchQuery.queryToMapEntries(uri))
                .withRequiredParameters(FROM, SIZE, SORT)
                .build()
                .doSearch(openSearchSwsClient);

        assertNotNull(pagedSearchResourceDto);
        logger.info("id: " + pagedSearchResourceDto.id());

        var nextId = getMapFromUri(pagedSearchResourceDto.nextResults());
        if (!nextId.isEmpty()) {
            var id = getMapFromUri(pagedSearchResourceDto.id());
            assertEquals(id.containsKey(FROM.key()), nextId.containsKey(FROM.key()));
            assertEquals(id.get(SIZE.key()), nextId.get(SIZE.key()));

            var size = Integer.parseInt(id.get(SIZE.key()));
            var offsetId =  Long.parseLong(id.get(FROM.key()));
            var nextOffsetId = Long.parseLong(nextId.get(FROM.key()));
            assertNotEquals(offsetId, nextOffsetId);
            assertEquals(offsetId + size, nextOffsetId);

            logger.info("nextId: {}", pagedSearchResourceDto.nextResults());
        }
    }


    @ParameterizedTest
    @MethodSource("uriSortingProvider")
    void uriParamsToResourceParams(URI uri) throws ApiGatewayException {
        var resourceSwsQuery =
            ResourceSwsQuery.Builder
                .queryBuilder()
                .fromQueryParameters(OpenSearchQuery.queryToMapEntries(uri))
                .withRequiredParameters(FROM, SIZE, SORT)
                .build();
        var pagedSearchResourceDto =
                resourceSwsQuery.doSearch(openSearchSwsClient);
        assertNotNull(resourceSwsQuery.getValue(CATEGORY).as());
        assertNotNull(resourceSwsQuery.removeValue(CATEGORY));
        assertNull(resourceSwsQuery.removeValue(CATEGORY));
        assertNotNull(pagedSearchResourceDto.id());
        assertNotNull(pagedSearchResourceDto.context());
    }


    static Stream<URI> uriSortingProvider() {
        return Stream.of(
            URI.create("https://example.com/?category=PhdThesis&sort=fieldName1&sortOrder=asc&sort=fieldName2&order"
                       + "=desc"),
            URI.create("https://example.com/?category=PhdThesis"),
            URI.create("https://example.com/?category=PhdThesis&orderBy=fieldName1:asc,fieldName2:desc"),
            URI.create("https://example.com/?category=PhdThesis&sort=fieldName1+asc&sort=fieldName2+desc"));
    }

    static Stream<URI> uriProvider() {
        return Stream.of(
            URI.create("https://example.com/testsearch?category=hello+world&lang=en&size=2&from=2"),
            URI.create("https://example.com/testsearch?title=hello+world&modified_before=2019-01-01"),
            URI.create("https://example.com/testsearch?contributor=hello+world&published_before=2020"),
            URI.create("https://example.com/testsearch?user=hello+world&size=2&from=0"));
    }

    private Map<String,String> getMapFromUri(URI uri) {
        if (nonNull(uri)) {
            return queryToMapEntries(uri).stream().collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
        } else {
            return Collections.emptyMap();
        }
    }
}