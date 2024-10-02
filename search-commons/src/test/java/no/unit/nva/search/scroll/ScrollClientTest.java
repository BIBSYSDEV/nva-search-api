package no.unit.nva.search.scroll;

import static no.unit.nva.indexing.testutils.MockedJwtProvider.setupMockedCachedJwtProvider;
import static no.unit.nva.search.common.MockedHttpResponse.mockedFutureHttpResponse;
import static no.unit.nva.testutils.RandomDataGenerator.randomString;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.http.HttpClient;
import java.nio.file.Path;

class ScrollClientTest {

    public static final String SAMPLE_PUBLICATION_SEARCH = "publication_response.json";
    private static final Logger logger = LoggerFactory.getLogger(ScrollClientTest.class);
    private ScrollClient scrollClient;

    @BeforeEach
    public void setUp() {
        var httpClient = mock(HttpClient.class);
        var cachedJwtProvider = setupMockedCachedJwtProvider();
        scrollClient = new ScrollClient(httpClient, cachedJwtProvider);
        when(httpClient.sendAsync(any(), any()))
                .thenReturn(mockedFutureHttpResponse(Path.of(SAMPLE_PUBLICATION_SEARCH)));
    }

    @Test
    void searchWithUriReturnsOpenSearchAwsResponse() {
        var scrollId = randomString();
        var resourceAwsQuery = new ScrollQuery(scrollId, "1m");
        var result = scrollClient.doSearch(resourceAwsQuery);
        logger.debug(result.toString());
        assertNotNull(result);
        assertNotNull(ScrollParameter.INVALID.asCamelCase());
        assertNotNull(ScrollParameter.INVALID.asLowerCase());
        assertNotNull(ScrollParameter.INVALID.errorMessage());
        assertNotNull(ScrollParameter.INVALID.fieldBoost());
        assertNotNull(ScrollParameter.INVALID.fieldPattern());
        assertNotNull(ScrollParameter.INVALID.fieldPattern());
        assertNotNull(ScrollParameter.INVALID.fieldType());
        assertNotNull(ScrollParameter.INVALID.searchOperator());
        assertNotNull(ScrollParameter.INVALID.searchFields(true));
        assertNotNull(ScrollParameter.INVALID.valueEncoding());
        assertNotNull(ScrollParameter.INVALID.valuePattern());
    }
}
