package no.unit.nva.search2;

import static no.unit.nva.indexing.testutils.MockedJwtProvider.setupMockedCachedJwtProvider;
import static no.unit.nva.search2.OpenSearchSwsClientTest.mockedHttpResponse;
import static no.unit.nva.search2.model.ResourceParameterKey.CATEGORY;
import static no.unit.nva.search2.model.ResourceParameterKey.FROM;
import static no.unit.nva.search2.model.ResourceParameterKey.SIZE;
import static no.unit.nva.search2.model.ResourceParameterKey.SORT;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.util.stream.Stream;
import no.unit.nva.search2.model.OpenSearchQuery;
import nva.commons.apigateway.exceptions.ApiGatewayException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

class OpenSearchSwsClientNoHitsTest {

    private OpenSearchSwsClient openSearchSwsClient;


    private static final String NO_HITS_RESPONSE_JSON = "no_hits_response.json";

    @BeforeEach
    public void setUp() throws IOException, InterruptedException {

        var httpClient = mock(HttpClient.class);
        var cachedJwtProvider = setupMockedCachedJwtProvider();
        openSearchSwsClient = new OpenSearchSwsClient(cachedJwtProvider, httpClient);
        var response = mockedHttpResponse(NO_HITS_RESPONSE_JSON);
        when(httpClient.send(any(), any()))
            .thenReturn(response);
    }


    @ParameterizedTest
    @MethodSource("uriProvider")
    void searchSingleTermReturnsOpenSearchSwsResponse(URI uri) throws ApiGatewayException {

        var pagedSearchResourceDto =
            ResourceSwsQuery.Builder
                .queryBuilder()
                .fromQueryParameters(OpenSearchQuery.queryToMapEntries(uri))
                .withRequiredParameters(FROM, SIZE, SORT)
                .build()
                .doSearch(openSearchSwsClient);

        assertNotNull(pagedSearchResourceDto);
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
        assertNotNull(resourceSwsQuery.getValue(CATEGORY).as());
        assertNotNull(resourceSwsQuery.removeValue(CATEGORY));
        assertNull(resourceSwsQuery.removeValue(CATEGORY));
        var pagedSearchResourceDto =
            resourceSwsQuery.doSearch(openSearchSwsClient);
        assertNotNull(pagedSearchResourceDto.id());
        assertNotNull(pagedSearchResourceDto.context());
        assertEquals(0L, pagedSearchResourceDto.totalHits());
        assertEquals(0, pagedSearchResourceDto.hits().size());
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
            URI.create("https://example.com/testsearch?category=hello+world&lang=en"),
            URI.create("https://example.com/testsearch?title=hello+world&modified_before=2019-01-01"),
            URI.create("https://example.com/testsearch?contributor=hello+world&published_before=2020"),
            URI.create("https://example.com/testsearch?user=hello+world&lang=en"));
    }
}