package no.unit.nva.search;

import static no.unit.nva.indexing.testutils.MockedJwtProvider.setupMockedCachedJwtProvider;
import static no.unit.nva.search.common.MockedHttpResponse.mockedFutureHttpResponse;
import static no.unit.nva.testutils.RandomDataGenerator.randomString;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.net.http.HttpClient;
import java.nio.file.Path;

import no.unit.nva.search.scroll.ScrollClient;
import no.unit.nva.search.scroll.ScrollParameters;
import no.unit.nva.search.scroll.ScrollQuery;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
        assertNotNull(ScrollParameters.INVALID.asCamelCase());
        assertNotNull(ScrollParameters.INVALID.asLowerCase());
        assertNotNull(ScrollParameters.INVALID.errorMessage());
        assertNotNull(ScrollParameters.INVALID.fieldBoost());
        assertNotNull(ScrollParameters.INVALID.fieldPattern());
        assertNotNull(ScrollParameters.INVALID.fieldPattern());
        assertNotNull(ScrollParameters.INVALID.fieldType());
        assertNotNull(ScrollParameters.INVALID.searchOperator());
        assertNotNull(ScrollParameters.INVALID.searchFields(true));
        assertNotNull(ScrollParameters.INVALID.valueEncoding());
        assertNotNull(ScrollParameters.INVALID.valuePattern());
    }

}