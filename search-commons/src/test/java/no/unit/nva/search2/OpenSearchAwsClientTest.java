package no.unit.nva.search2;

import static no.unit.nva.search2.model.OpenSearchQuery.queryToMap;
import static no.unit.nva.search2.model.ResourceParameterKey.FROM;
import static no.unit.nva.search2.model.ResourceParameterKey.SIZE;
import static no.unit.nva.search2.model.ResourceParameterKey.SORT;
import static nva.commons.core.ioutils.IoUtils.stringFromResources;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import com.fasterxml.jackson.core.JsonProcessingException;
import java.net.URI;
import java.nio.file.Path;
import java.util.stream.Stream;
import no.unit.nva.search2.constant.Defaults;
import nva.commons.apigateway.exceptions.ApiGatewayException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.opensearch.action.search.SearchResponse;

class OpenSearchAwsClientTest {

    private OpenSearchAwsClient openSearchAwsClient;

    public static final String SAMPLE_OPENSEARCH_RESPONSE_RESPONSE_EXPORT
        = "sample_opensearch_response.json";

    @BeforeEach
    public void setUp() throws JsonProcessingException {

        openSearchAwsClient =  mock(OpenSearchAwsClient.class);
        var resource = stringFromResources(Path.of(SAMPLE_OPENSEARCH_RESPONSE_RESPONSE_EXPORT));
        var mockedResponse = Defaults.objectMapperWithEmpty.readValue(resource, SearchResponse.class);

        when(openSearchAwsClient.doSearch(any(), any())).thenReturn(mockedResponse);

    }

    @ParameterizedTest
    @MethodSource("uriProvider")
    void searchSingleTermReturnsOpenSearchAwsResponse(URI uri) throws ApiGatewayException {
        var pagedSearchResourceDto =
            ResourceAwsQuery.Builder
                .queryBuilder()
                .fromQueryParameters(queryToMap(uri))
                .withRequiredParameters(FROM, SIZE, SORT)
                .build()
                .doSearch(openSearchAwsClient);

        assertNotNull(pagedSearchResourceDto);
    }


    @ParameterizedTest
    @MethodSource("uriSortingProvider")
    void uriParamsToResourceParams(URI uri) throws ApiGatewayException {
        var pagedSearchResourceDto =
            ResourceAwsQuery.Builder
                .queryBuilder()
                .fromQueryParameters(queryToMap(uri))
                .withRequiredParameters(FROM, SIZE, SORT)
                .build()
                .doSearch(openSearchAwsClient);
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
            URI.create("https://example.com/testsearch?category=hello+world&lang=en"),
            URI.create("https://example.com/testsearch?title=hello+world&lang=en"),
            URI.create("https://example.com/testsearch?contributor=hello+world&lang=en"),
            URI.create("https://example.com/testsearch?user=hello+world&lang=en"));
    }
}