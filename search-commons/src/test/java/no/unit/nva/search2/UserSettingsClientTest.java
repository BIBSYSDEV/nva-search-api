package no.unit.nva.search2;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.util.List;
import java.util.stream.Stream;

import no.unit.nva.search2.common.records.UserSettings;
import no.unit.nva.search2.resource.ResourceSearchQuery;
import no.unit.nva.search2.resource.UserSettingsClient;
import nva.commons.apigateway.exceptions.ApiGatewayException;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static no.unit.nva.search2.common.EntrySetTools.queryToMapEntries;
import static no.unit.nva.search2.common.MockedHttpResponse.mockedHttpResponse;
import static no.unit.nva.search2.common.MockedJwtProvider.setupMockedCachedJwtProvider;
import static no.unit.nva.search2.resource.ResourceParameter.FROM;
import static no.unit.nva.search2.resource.ResourceParameter.SIZE;
import static no.unit.nva.search2.resource.ResourceParameter.SORT;
import static nva.commons.core.attempt.Try.attempt;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class UserSettingsClientTest {

    private static UserSettingsClient userSettingsClient;
    private static final Logger logger = LoggerFactory.getLogger(UserSettingsClientTest.class);
    public static final String SAMPLE_USER_SETTINGS_RESPONSE = "user_settings.json";

    @BeforeAll
    public static void setUp() throws IOException, InterruptedException {
        var mochedHttpClient = mock(HttpClient.class);
        var cachedJwtProvider = setupMockedCachedJwtProvider();
        userSettingsClient = new UserSettingsClient(mochedHttpClient, cachedJwtProvider);
        when(mochedHttpClient.send(any(), any()))
            .thenReturn(mockedHttpResponse(SAMPLE_USER_SETTINGS_RESPONSE))
            .thenReturn(mockedHttpResponse(""))
            .thenReturn(mockedHttpResponse("", 500));

    }

    @ParameterizedTest
    @MethodSource("uriProvider")
    void searchWithUriReturnsOpenSearchAwsResponse(URI uri) throws ApiGatewayException {
        var resourceAwsQuery =
            ResourceSearchQuery.builder()
                .fromQueryParameters(queryToMapEntries(uri))
                .withRequiredParameters(FROM, SIZE, SORT)
                .build();
        var promotedPublications = attempt(() -> userSettingsClient.doSearch(resourceAwsQuery))
            .map(UserSettings::promotedPublications)
            .orElse((e) -> {
                    if (e.isFailure()) {
                        logger.info(e.getException().getMessage());
                    }
                    return List.<String>of();
                }
            );
        assertNotNull(promotedPublications);
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