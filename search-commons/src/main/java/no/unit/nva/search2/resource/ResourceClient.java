package no.unit.nva.search2.resource;

import static java.net.HttpURLConnection.HTTP_OK;
import static no.unit.nva.commons.json.JsonUtils.singleLineObjectMapper;
import static nva.commons.core.attempt.Try.attempt;
import java.net.http.HttpClient;
import java.net.http.HttpResponse;
import no.unit.nva.search.CachedJwtProvider;
import no.unit.nva.search2.common.OpenSearchClient;
import no.unit.nva.search2.common.SwsResponse;
import nva.commons.core.JacocoGenerated;
import nva.commons.secrets.SecretsReader;

public class ResourceClient extends OpenSearchClient<SwsResponse, ResourceQuery> {

    private final UserSettingsClient userSettingsClient;

    public ResourceClient(HttpClient client, CachedJwtProvider cachedJwtProvider) {
        super(client, cachedJwtProvider);
        this.userSettingsClient = new UserSettingsClient(client, cachedJwtProvider);
    }

    public ResourceClient(HttpClient client, UserSettingsClient userSettingsClient,
                          CachedJwtProvider cachedJwtProvider) {
        super(client, cachedJwtProvider);
        this.userSettingsClient = userSettingsClient;
    }

    @JacocoGenerated
    public static ResourceClient defaultClient() {
        var cachedJwtProvider = getCachedJwtProvider(new SecretsReader());
        return new ResourceClient(HttpClient.newHttpClient(), cachedJwtProvider);
    }

    @Override
    public SwsResponse doSearch(ResourceQuery query) {
        return
            query.createQueryBuilderStream(userSettingsClient)
                .map(this::createRequest)
                .map(this::fetch)
                .map(this::handleResponse)
                .findFirst().orElseThrow();
    }

    @Override
    protected SwsResponse handleResponse(HttpResponse<String> response) {
        if (response.statusCode() != HTTP_OK) {
            throw new RuntimeException(response.body());
        }
        return attempt(() -> singleLineObjectMapper.readValue(response.body(), SwsResponse.class))
            .map(logAndReturnResult())
            .orElseThrow();
    }
}
