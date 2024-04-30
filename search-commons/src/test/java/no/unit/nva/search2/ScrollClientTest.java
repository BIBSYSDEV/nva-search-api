package no.unit.nva.search2;

import static no.unit.nva.indexing.testutils.MockedJwtProvider.setupMockedCachedJwtProvider;
import static no.unit.nva.search2.common.EntrySetTools.queryToMapEntries;
import static no.unit.nva.search2.common.MockedHttpResponse.mockedHttpResponse;
import static no.unit.nva.testutils.RandomDataGenerator.randomString;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import java.io.IOException;
import java.net.http.HttpClient;
import no.unit.nva.search2.scroll.ScrollClient;
import no.unit.nva.search2.scroll.ScrollQuery;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
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
        var resourceAwsQuery = new ScrollQuery(scrollId, "1m");
        var result = scrollClient.doSearch(resourceAwsQuery);
        logger.debug(result.toString());
        assertNotNull(result);
    }

}