package no.unit.nva.search2;

import static no.unit.nva.indexing.testutils.MockedJwtProvider.setupMockedCachedJwtProvider;
import static no.unit.nva.search2.common.EntrySetTools.queryToMapEntries;
import static no.unit.nva.search2.common.MockedHttpResponse.mockedHttpResponse;
import static no.unit.nva.search2.resource.ResourceParameter.FROM;
import static no.unit.nva.search2.resource.ResourceParameter.SIZE;
import static no.unit.nva.search2.resource.ResourceParameter.SORT;
import static no.unit.nva.testutils.RandomDataGenerator.randomString;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.util.stream.Stream;
import no.unit.nva.search2.common.scroll.ScrollClient;
import no.unit.nva.search2.common.scroll.ScrollQuery;
import no.unit.nva.search2.resource.ResourceSearchQuery;
import no.unit.nva.testutils.RandomDataGenerator;
import nva.commons.apigateway.exceptions.ApiGatewayException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

class ScrollClientTest {

    private ScrollClient scrollClient;
    private static final Logger logger = LoggerFactory.getLogger(ScrollClientTest.class);
    public static final String SAMPLE_PUBLICATION_SEARCH = "publication_response.json";

    @BeforeEach
    public void setUp() throws IOException, InterruptedException {
        var httpClient = mock(HttpClient.class);
        var cachedJwtProvider = setupMockedCachedJwtProvider();
        scrollClient = new ScrollClient(httpClient, cachedJwtProvider);
        when(httpClient.send(any(), any()))
            .thenReturn(mockedHttpResponse(SAMPLE_PUBLICATION_SEARCH));
    }

    @Test
    void searchWithUriReturnsOpenSearchAwsResponse() {
        var scrollId = randomString();
        var resourceAwsQuery =
            ScrollQuery.forScrollId(scrollId);
        var result = scrollClient.doSearch(resourceAwsQuery);
        logger.debug(result.toString());
        assertNotNull(result);
    }

}