package no.unit.nva.search2.resource;

import static java.net.HttpURLConnection.HTTP_OK;
import static no.unit.nva.auth.AuthorizedBackendClient.AUTHORIZATION_HEADER;
import static no.unit.nva.auth.AuthorizedBackendClient.CONTENT_TYPE;
import static no.unit.nva.commons.json.JsonUtils.singleLineObjectMapper;
import static no.unit.nva.auth.uriretriever.UriRetriever.ACCEPT;
import static no.unit.nva.search2.common.constant.Functions.readApiHost;
import static no.unit.nva.search2.common.constant.Words.HTTPS;
import static no.unit.nva.search2.resource.Constants.PERSON_PREFERENCES;
import static no.unit.nva.search2.resource.ResourceParameter.CONTRIBUTOR;
import static nva.commons.core.attempt.Try.attempt;
import com.google.common.net.MediaType;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.Charset;
import java.time.Instant;
import java.util.List;
import java.util.stream.Stream;

import no.unit.nva.commons.json.JsonSerializable;
import no.unit.nva.search2.common.jwt.CachedJwtProvider;
import no.unit.nva.search2.common.OpenSearchClient;
import no.unit.nva.search2.common.records.UserSettings;
import nva.commons.core.attempt.FunctionWithException;

public class UserSettingsClient extends OpenSearchClient<UserSettings, ResourceSearchQuery> {

    private URI userSettingUri;

    public UserSettingsClient(HttpClient client, CachedJwtProvider cachedJwtProvider) {
        super(client, cachedJwtProvider);
    }

    @Override
    public UserSettings doSearch(ResourceSearchQuery query) {
        queryBuilderStart = Instant.now();
        queryParameters = "";
        return
            createQueryBuilderStream(query)
                .map(this::createRequest)
                .map(this::fetch)
                .map(this::handleResponse)
                .findFirst().orElseThrow();
    }

    private Stream<String> createQueryBuilderStream(ResourceSearchQuery query) {
        return query.parameters().get(CONTRIBUTOR).asStream();
    }

    private HttpRequest createRequest(String contributorId) {
        var personId = URLEncoder.encode(contributorId, Charset.defaultCharset());
        userSettingUri = URI.create(HTTPS + readApiHost() + PERSON_PREFERENCES + personId);
        return HttpRequest
            .newBuilder(userSettingUri)
            .headers(
                ACCEPT, MediaType.JSON_UTF_8.toString(),
                CONTENT_TYPE, MediaType.JSON_UTF_8.toString(),
                AUTHORIZATION_HEADER, jwtProvider.getValue().getToken())
            .GET().build();
    }


    @Override
    protected UserSettings handleResponse(HttpResponse<String> response) {
        if (response.statusCode() != HTTP_OK) {
            throw new RuntimeException("Error fetching user settings: " + response.body());
        }

        return
            attempt(() -> singleLineObjectMapper.readValue(response.body(), UserSettings.class))
                .map(logAndReturnUserSettings())
                .orElseThrow();
    }

    protected FunctionWithException<UserSettings, UserSettings, RuntimeException> logAndReturnUserSettings() {
        return result -> {
            logger.info(new UserSettingLog(userSettingUri, result).toJsonString());
            return result;
        };
    }

    record UserSettingLog(URI uri, List<String> promotedPublications) implements JsonSerializable {
        public UserSettingLog(URI uri, UserSettings userSettings) {
            this(uri, userSettings.promotedPublications());
        }
    }
}