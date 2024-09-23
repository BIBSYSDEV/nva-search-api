package no.unit.nva.search.resource;

import static no.unit.nva.auth.AuthorizedBackendClient.AUTHORIZATION_HEADER;
import static no.unit.nva.auth.uriretriever.UriRetriever.ACCEPT;
import static no.unit.nva.commons.json.JsonUtils.singleLineObjectMapper;
import static no.unit.nva.constants.Words.CONTENT_TYPE;
import static no.unit.nva.constants.Words.HTTPS;
import static no.unit.nva.search.common.constant.Functions.readApiHost;
import static no.unit.nva.search.resource.Constants.PERSON_PREFERENCES;
import static no.unit.nva.search.resource.ResourceParameter.CONTRIBUTOR;

import static nva.commons.core.StringUtils.EMPTY_STRING;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.google.common.net.MediaType;

import no.unit.nva.commons.json.JsonSerializable;
import no.unit.nva.search.common.OpenSearchClient;
import no.unit.nva.search.common.jwt.CachedJwtProvider;
import no.unit.nva.search.common.records.UserSettings;

import nva.commons.core.JacocoGenerated;
import nva.commons.core.attempt.FunctionWithException;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.Charset;
import java.time.Instant;
import java.util.List;
import java.util.function.BinaryOperator;
import java.util.stream.Stream;

/**
 * @author Stig Norland
 */
public class UserSettingsClient extends OpenSearchClient<UserSettings, ResourceSearchQuery> {

    private URI userSettingUri;

    public UserSettingsClient(HttpClient client, CachedJwtProvider cachedJwtProvider) {
        super(client, cachedJwtProvider);
    }

    @Override
    public UserSettings doSearch(ResourceSearchQuery query) {
        queryBuilderStart = Instant.now();
        queryParameters = EMPTY_STRING;
        return createQueryBuilderStream(query)
                .map(this::createRequest)
                .map(super::fetch)
                .map(super::handleResponse)
                .findFirst()
                .orElseThrow()
                .join();
    }

    @Override
    protected UserSettings jsonToResponse(HttpResponse<String> response)
            throws JsonProcessingException {
        return singleLineObjectMapper.readValue(response.body(), UserSettings.class);
    }

    @Override
    @JacocoGenerated
    protected BinaryOperator<UserSettings> responseAccumulator() {
        // not in use
        return null;
    }

    @Override
    protected FunctionWithException<UserSettings, UserSettings, RuntimeException>
            logAndReturnResult() {
        return result -> {
            logger.info(new UserSettingLog(userSettingUri, result).toJsonString());
            return result;
        };
    }

    private Stream<String> createQueryBuilderStream(ResourceSearchQuery query) {
        return query.parameters().get(CONTRIBUTOR).asStream();
    }

    private HttpRequest createRequest(String contributorId) {
        var personId = URLEncoder.encode(contributorId, Charset.defaultCharset());
        userSettingUri = URI.create(HTTPS + readApiHost() + PERSON_PREFERENCES + personId);
        return HttpRequest.newBuilder(userSettingUri)
                .headers(
                        ACCEPT, MediaType.JSON_UTF_8.toString(),
                        CONTENT_TYPE, MediaType.JSON_UTF_8.toString(),
                        AUTHORIZATION_HEADER, jwtProvider.getValue().getToken())
                .GET()
                .build();
    }

    private record UserSettingLog(URI uri, List<String> promotedPublications)
            implements JsonSerializable {
        public UserSettingLog(URI uri, UserSettings userSettings) {
            this(uri, userSettings.promotedPublications());
        }
    }
}
