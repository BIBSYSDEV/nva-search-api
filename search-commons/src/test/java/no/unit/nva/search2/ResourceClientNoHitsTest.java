package no.unit.nva.search2;

import static no.unit.nva.indexing.testutils.MockedJwtProvider.setupMockedCachedJwtProvider;
import static no.unit.nva.search2.common.MockedHttpResponse.mockedHttpResponse;
import static no.unit.nva.search2.common.QueryTools.queryToMapEntries;
import static no.unit.nva.search2.enums.ResourceParameter.FROM;
import static no.unit.nva.search2.enums.ResourceParameter.INSTANCE_TYPE;
import static no.unit.nva.search2.enums.ResourceParameter.SIZE;
import static no.unit.nva.search2.enums.ResourceParameter.SORT;
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
import nva.commons.apigateway.exceptions.ApiGatewayException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

class ResourceClientNoHitsTest {

    private ResourceClient resourceClient;

    private static final String NO_HITS_RESPONSE_JSON = "no_hits_response.json";

    @BeforeEach
    public void setUp() throws IOException, InterruptedException {

        var httpClient = mock(HttpClient.class);
        var cachedJwtProvider = setupMockedCachedJwtProvider();
        resourceClient = new ResourceClient(httpClient, cachedJwtProvider);
        var response = mockedHttpResponse(NO_HITS_RESPONSE_JSON);
        when(httpClient.send(any(), any()))
            .thenReturn(response);
    }

    @ParameterizedTest
    @MethodSource("uriProvider")
    void searchSingleTermReturnsOpenSearchSwsResponse(URI uri) throws ApiGatewayException {

        var pagedSearchResourceDto =
            ResourceQuery.builder()
                .fromQueryParameters(queryToMapEntries(uri))
                .withRequiredParameters(FROM, SIZE, SORT)
                .build()
                .doSearch(resourceClient);

        assertNotNull(pagedSearchResourceDto);
    }

    @ParameterizedTest
    @MethodSource("uriSortingProvider")
    void uriParamsToResourceParams(URI uri) throws ApiGatewayException {
        var query =
            ResourceQuery.builder()
                .fromQueryParameters(queryToMapEntries(uri))
                .withRequiredParameters(FROM, SIZE, SORT)
                .build();
        assertNotNull(query.getValue(INSTANCE_TYPE).as());
        assertNotNull(query.removeKey(INSTANCE_TYPE));
        assertNull(query.removeKey(INSTANCE_TYPE));
        var response = resourceClient.doSearch(query);
        var pagedSearchResourceDto = query.toPagedResponse(response);

        assertNotNull(pagedSearchResourceDto.id());
        assertNotNull(pagedSearchResourceDto.context());
        assertEquals(0L, pagedSearchResourceDto.totalHits());
        assertEquals(0, pagedSearchResourceDto.hits().size());
    }

    static Stream<URI> uriSortingProvider() {
        return Stream.of(
            URI.create("https://example.com/?INSTANCE_TYPE=PhdThesis&sort=INSTANCE_TYPE&sortOrder=asc&sort=created_date&order"
                       + "=desc"),
            URI.create("https://example.com/?INSTANCE_TYPE=PhdThesis"),
            URI.create("https://example.com/?INSTANCE_TYPE=PhdThesis&orderBy=INSTANCE_TYPE:asc,created_date:desc"),
            URI.create("https://example.com/?INSTANCE_TYPE=PhdThesis&sort=INSTANCE_TYPE+asc&sort=created_date+desc"));
    }

    static Stream<URI> uriProvider() {
        return Stream.of(
            URI.create("https://example.com/testsearch?INSTANCE_TYPE=hello+world&lang=en"),
            URI.create("https://example.com/testsearch?title=hello+world&modified_before=2019-01-01"),
            URI.create("https://example.com/testsearch?contributor=hello+world&published_before=2020-01-01"),
            URI.create("https://example.com/testsearch?user=hello+world&lang=en"));
    }
}