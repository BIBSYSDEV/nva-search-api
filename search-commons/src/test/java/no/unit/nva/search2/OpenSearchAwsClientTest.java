package no.unit.nva.search2;

import static no.unit.nva.indexing.testutils.MockedJwtProvider.setupMockedCachedJwtProvider;
import static no.unit.nva.indexing.testutils.SearchResponseUtil.getSearchResponseFromJson;
import static no.unit.nva.search2.model.ResourceParameterKey.FROM;
import static no.unit.nva.search2.model.ResourceParameterKey.SIZE;
import static no.unit.nva.search2.model.ResourceParameterKey.SORT;
import static nva.commons.core.ioutils.IoUtils.stringFromResources;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import java.io.IOException;
import java.net.URI;
import java.nio.file.Path;
import java.util.stream.Stream;
import no.unit.nva.search2.model.OpenSearchQuery;
import nva.commons.apigateway.exceptions.ApiGatewayException;
import nva.commons.apigateway.exceptions.BadRequestException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.opensearch.client.RestHighLevelClient;

class OpenSearchAwsClientTest {

    private OpenSearchAwsClient openSearchAwsClient;

    public static final String SAMPLE_OPENSEARCH_RESPONSE_RESPONSE_EXPORT
        = "sample_opensearch_response.json";


    @BeforeEach
    public void setUp() throws IOException {
        var mockedOpenSearchClient = mock(RestHighLevelClient.class);
        var mockedResponse = generateMockSearchResponse(SAMPLE_OPENSEARCH_RESPONSE_RESPONSE_EXPORT);
        openSearchAwsClient = new OpenSearchAwsClient(setupMockedCachedJwtProvider(), mockedOpenSearchClient);
        when(mockedOpenSearchClient.search(any(), any()))
            .thenReturn(mockedResponse);
    }


    @ParameterizedTest
    @MethodSource("uriProvider")
    void searchWithUriReturnsOpenSearchAwsResponse(URI uri) throws ApiGatewayException {
        var pagedSearchResourceDto =
            ResourceAwsQuery.Builder
                .queryBuilder()
                .fromQueryParameters(OpenSearchQuery.queryToMapEntries(uri))
                .withRequiredParameters(FROM, SIZE, SORT)
                .build()
                .doSearch(openSearchAwsClient);

        assertNotNull(pagedSearchResourceDto);
    }


    @ParameterizedTest
    @MethodSource("uriSortingProvider")
    void searchUriWithSortingReturnsOpenSearchAwsResponse(URI uri) throws ApiGatewayException {
        var pagedSearchResourceDto =
            ResourceAwsQuery.Builder
                .queryBuilder()
                .fromQueryParameters(OpenSearchQuery.queryToMapEntries(uri))
                .withRequiredParameters(FROM, SIZE, SORT)
                .build()
                .doSearch(openSearchAwsClient);
        assertNotNull(pagedSearchResourceDto.id());
        assertNotNull(pagedSearchResourceDto.context());
        assertTrue(pagedSearchResourceDto.id().getScheme().contains("https"));
    }


    @ParameterizedTest
    @MethodSource("uriInvalidProvider")
    void failToSearchUri(URI uri) {
        assertThrows(BadRequestException.class,
                     () -> ResourceAwsQuery.Builder
                               .queryBuilder()
                               .fromQueryParameters(OpenSearchQuery.queryToMapEntries(uri))
                               .withRequiredParameters(FROM, SIZE)
                               .build()
                               .doSearch(openSearchAwsClient));
    }

    static Stream<URI> uriSortingProvider() {
        return Stream.of(
            URI.create("https://example.com/?category=PhdThesis&sort=title&sortOrder=asc&sort=category&order"
                + "=desc"),
            URI.create("https://example.com/?category=PhdThesis&sort=title&sortOrder=asc&sort=category"),
            URI.create("https://example.com/?category=PhdThesis&size=10&from=0&sort=category:desc"),
            URI.create("https://example.com/?category=PhdThesis&orderBy=contributorid:asc,funding:desc"),
            URI.create("https://example.com/?category=PhdThesis&orderBy=institutionName:asc," +
                "modifiedDate:desc&searchAfter=1241234,23412"),
            URI.create("https://example.com/?category=PhdThesis&sort=unit+asc&sort=id+desc"));
    }

    static Stream<URI> uriProvider() {
        return Stream.of(
            URI.create("https://example.com/testsearch?category=hello+world&lang=en&fields=category,title"),
            URI.create("https://example.com/testsearch?title=hello+world&modified_before=2019-01-01"),
            URI.create("https://example.com/testsearch?contributor=hello+world&published_before=2020"),
            URI.create("https://example.com/testsearch?user=hello+world&lang=en&fields=all"));
    }

    static Stream<URI> uriInvalidProvider() {
        return Stream.of(
            URI.create("https://example.com/testsearch?categories=hello+world&lang=en"),
            URI.create("https://example.com/testsearch?tittles=hello+world&modified_before=2019-01-01"),
            URI.create("https://example.com/testsearch?conttributors=hello+world&published_before=2020"),
            URI.create("https://example.com/?category=PhdThesis&sort=beunited+asc"),
            URI.create("https://example.com/testsearch?useers=hello+world&lang=en"));
    }


    private org.opensearch.action.search.SearchResponse generateMockSearchResponse(String filename) throws IOException {
        var json = stringFromResources(Path.of(filename));
        return getSearchResponseFromJson(json);
    }
}