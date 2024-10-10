package nva.search.scroll;

import static no.unit.nva.indexing.testutils.MockedJwtProvider.setupMockedCachedJwtProvider;
import static no.unit.nva.search.service.scroll.ScrollParameter.INVALID;
import static no.unit.nva.testutils.RandomDataGenerator.randomString;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import no.unit.nva.common.MockedHttpResponse;
import no.unit.nva.search.service.scroll.ScrollClient;
import no.unit.nva.search.service.scroll.ScrollQuery;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.http.HttpClient;
import java.nio.file.Path;

class ScrollClientTest {

    public static final String SAMPLE_PUBLICATION_SEARCH =
            "resource_mocked_sws_search_response.json";
    private static final Logger logger = LoggerFactory.getLogger(ScrollClientTest.class);
    private ScrollClient scrollClient;

    @BeforeEach
    public void setUp() {
        var httpClient = mock(HttpClient.class);
        var cachedJwtProvider = setupMockedCachedJwtProvider();
        scrollClient = new ScrollClient(httpClient, cachedJwtProvider);
        when(httpClient.sendAsync(any(), any()))
                .thenReturn(MockedHttpResponse.mockedFutureHttpResponse(Path.of(SAMPLE_PUBLICATION_SEARCH)));
    }

    @Test
    void searchWithUriReturnsOpenSearchAwsResponse() {
        var scrollId = randomString();
        var resourceAwsQuery = new ScrollQuery(scrollId, "1m");
        var result = scrollClient.doSearch(resourceAwsQuery);
        logger.debug(result.toString());
        assertNotNull(result);
        assertNotNull(INVALID.asCamelCase());
        assertNotNull(INVALID.asLowerCase());
        assertNotNull(INVALID.errorMessage());
        assertNotNull(INVALID.fieldBoost());
        assertNotNull(INVALID.fieldPattern());
        assertNotNull(INVALID.fieldPattern());
        assertNotNull(INVALID.fieldType());
        assertNotNull(INVALID.searchOperator());
        assertNotNull(INVALID.searchFields(true));
        assertNotNull(INVALID.valueEncoding());
        assertNotNull(INVALID.valuePattern());
    }
}
