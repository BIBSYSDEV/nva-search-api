package no.unit.nva.search2;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.util.stream.Stream;
import no.unit.nva.search2.resource.ResourceQuery;
import no.unit.nva.search2.resource.UserSettingsClient;
import nva.commons.apigateway.exceptions.ApiGatewayException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static no.unit.nva.indexing.testutils.MockedJwtProvider.setupMockedCachedJwtProvider;
import static no.unit.nva.search2.common.EntrySetTools.queryToMapEntries;
import static no.unit.nva.search2.common.MockedHttpResponse.mockedHttpResponse;
import static no.unit.nva.search2.resource.ResourceParameter.FROM;
import static no.unit.nva.search2.resource.ResourceParameter.SIZE;
import static no.unit.nva.search2.resource.ResourceParameter.SORT;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class UserSettingsClientTest {

    private UserSettingsClient userSettingsClient;
    private static final Logger logger = LoggerFactory.getLogger(UserSettingsClientTest.class);
    public static final String SAMPLE_USER_SETTINGS_RESPONSE = "user_settings.json";

    @BeforeEach
    public void setUp() throws IOException, InterruptedException {
        var httpClient = mock(HttpClient.class);
        var cachedJwtProvider = setupMockedCachedJwtProvider();
        userSettingsClient = new UserSettingsClient(httpClient, cachedJwtProvider);
        when(httpClient.send(any(), any()))
            .thenReturn(mockedHttpResponse(SAMPLE_USER_SETTINGS_RESPONSE));
    }

    @ParameterizedTest
    @MethodSource("uriProvider")
    void searchWithUriReturnsOpenSearchAwsResponse(URI uri) throws ApiGatewayException {
        var resourceAwsQuery =
            ResourceQuery.builder()
                .fromQueryParameters(queryToMapEntries(uri))
                .withRequiredParameters(FROM, SIZE, SORT)
                .build();
        var result = userSettingsClient.doSearch(resourceAwsQuery).promotedPublications();
        logger.debug(result.toString());
        assertNotNull(result);
    }

    static Stream<URI> uriProvider() {
        return Stream.of(
            URI.create("https://example.com/?contributor=http://hello.worl.test.orgd&modified_before=2019-01-01"),
            URI.create("https://example.com/?contributor=https://api.dev.nva.aws.unit.no/cristin/person/1269057"),
            URI.create(
                "https://example.com/?contributor=https%3A%2F%2Fapi.dev.nva.aws.unit"
                + ".no%2Fcristin%2Fperson%2F1269057&orderBy=UNIT_ID:asc,title:desc"),
            URI.create("https://example.com/?query=hello+world&fields=all"));
    }
}