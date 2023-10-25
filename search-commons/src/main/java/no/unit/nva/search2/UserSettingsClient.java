package no.unit.nva.search2;

import com.google.common.net.MediaType;
import no.unit.nva.search.CachedJwtProvider;
import no.unit.nva.search2.model.UserSettings;
import no.unit.nva.search2.model.OpenSearchClient;
import nva.commons.core.JacocoGenerated;
import nva.commons.core.paths.UriWrapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.stream.Stream;

import static java.net.HttpURLConnection.HTTP_OK;
import static java.util.Objects.nonNull;
import static no.unit.nva.auth.AuthorizedBackendClient.AUTHORIZATION_HEADER;
import static no.unit.nva.commons.json.JsonUtils.singleLineObjectMapper;
import static no.unit.nva.search.utils.UriRetriever.ACCEPT;
import static no.unit.nva.search2.constant.ApplicationConstants.readApiHost;
import static no.unit.nva.search2.model.ResourceParameterKey.CONTRIBUTOR;
import static nva.commons.core.attempt.Try.attempt;

public class UserSettingsClient  implements OpenSearchClient<UserSettings, ResourceAwsQuery> {
    private static final Logger logger = LoggerFactory.getLogger(UserSettingsClient.class);
    private final CachedJwtProvider jwtProvider;
    private final HttpClient httpClient;
    private final HttpResponse.BodyHandler<String> bodyHandler;


    public UserSettingsClient(CachedJwtProvider cachedJwtProvider, HttpClient client) {
        super();
        this.jwtProvider = cachedJwtProvider;
        this.httpClient = client;
        this.bodyHandler = HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8);
    }

    @Override
    public UserSettings doSearch(ResourceAwsQuery query) {
        return
            createQueryBuilderStream(query)
                .map(this::populateSearchRequest)
                .map(this::createRequest)
                .map(this::fetch)
                .map(this::handleResponse)
                .findFirst()
                .orElse(new UserSettings(Collections.emptyList()));
}

    private Stream<String> createQueryBuilderStream(ResourceAwsQuery query) {
        if (query.isPresent(CONTRIBUTOR)) {
            return Stream.of(query.getValue(CONTRIBUTOR).toString());
        }
        return Stream.empty();
    }

    private URI populateSearchRequest(String contributorId) {
        return UriWrapper.fromHost(readApiHost())
            .addChild("person-preferences")
            .addChild(URLEncoder.encode(contributorId, StandardCharsets.UTF_8))
            .getUri();
    }

    @JacocoGenerated
    private HttpRequest createRequest(URI userSettingId) {
        logger.info(userSettingId.toString());
        return HttpRequest
            .newBuilder(userSettingId)
            .headers(
                ACCEPT, MediaType.JSON_UTF_8.toString(),
                AUTHORIZATION_HEADER, jwtProvider.getValue().getToken())
            .GET().build();
    }

    private HttpResponse<String> fetch(HttpRequest httpRequest) {
        return attempt(() -> httpClient.send(httpRequest, bodyHandler)).orElseThrow();
    }

    @JacocoGenerated
    private UserSettings handleResponse(HttpResponse<String> response) {
        if (response.statusCode() != HTTP_OK) {
            throw new RuntimeException(response.body());
        }
        var usersettings = attempt(() -> singleLineObjectMapper.readValue(response.body(), UserSettings.class))
            .orElseThrow();
        return nonNull(usersettings.promotedPublications()) ? usersettings : new UserSettings(Collections.emptyList());
    }

}
