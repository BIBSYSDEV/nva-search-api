package no.unit.nva.search2;

import static java.net.HttpURLConnection.HTTP_OK;
import static no.unit.nva.auth.AuthorizedBackendClient.AUTHORIZATION_HEADER;
import static no.unit.nva.auth.AuthorizedBackendClient.CONTENT_TYPE;
import static no.unit.nva.commons.json.JsonUtils.singleLineObjectMapper;
import static no.unit.nva.search.utils.UriRetriever.ACCEPT;
import static no.unit.nva.search2.constant.Functions.readApiHost;
import static no.unit.nva.search2.enums.ResourceParameter.CONTRIBUTOR;
import static nva.commons.core.attempt.Try.attempt;
import com.google.common.net.MediaType;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Collections;
import java.util.stream.Stream;
import no.unit.nva.search.CachedJwtProvider;
import no.unit.nva.search2.common.OpenSearchClient;
import no.unit.nva.search2.dto.UserSettings;
import nva.commons.core.JacocoGenerated;
import nva.commons.core.paths.UriWrapper;

public class UserSettingsClient extends OpenSearchClient<UserSettings, ResourceQuery> {

    public UserSettingsClient(HttpClient client, CachedJwtProvider cachedJwtProvider) {
        super(client, cachedJwtProvider);
    }

    @JacocoGenerated
    @Override
    public UserSettings doSearch(ResourceQuery query) {
        return
            createQueryBuilderStream(query)
                .map(this::createRequest)
                .map(this::fetch)
                .map(this::handleResponse)
                .findFirst()
                .orElse(new UserSettings(Collections.emptyList()));
    }

    private Stream<String> createQueryBuilderStream(ResourceQuery query) {
        return query.getValue(CONTRIBUTOR).optional().stream();
    }

    private HttpRequest createRequest(String contributorId) {
        var userSettingId = UriWrapper.fromHost(readApiHost())
            .addChild("person-preferences")
            .addChild(contributorId)
            .getUri();
        return HttpRequest
            .newBuilder(userSettingId)
            .headers(
                ACCEPT, MediaType.JSON_UTF_8.toString(),
                CONTENT_TYPE, MediaType.JSON_UTF_8.toString(),
                AUTHORIZATION_HEADER, jwtProvider.getValue().getToken())
            .GET().build();
    }

    @JacocoGenerated
    @Override
    protected UserSettings handleResponse(HttpResponse<String> response) {
        if (response.statusCode() != HTTP_OK) {
            logger.error("Error fetching user settings: {}", response.body());
            return new UserSettings(Collections.emptyList());
        }

        var settings =
            attempt(() -> singleLineObjectMapper.readValue(response.body(), UserSettings.class));
        return settings.isSuccess()
            ? settings.get()
            : new UserSettings(Collections.emptyList());
    }
}
