package no.unit.nva.search.scroll;

import static java.net.HttpURLConnection.HTTP_OK;
import static no.unit.nva.commons.json.JsonUtils.singleLineObjectMapper;
import static nva.commons.core.attempt.Try.attempt;
import java.net.http.HttpClient;
import java.net.http.HttpResponse;

import no.unit.nva.search.common.OpenSearchClient;
import no.unit.nva.search.common.jwt.CachedJwtProvider;
import no.unit.nva.search.common.records.SwsResponse;
import nva.commons.core.JacocoGenerated;
import nva.commons.secrets.SecretsReader;

public class ScrollClient extends OpenSearchClient<SwsResponse, ScrollQuery> {


    public ScrollClient(HttpClient client, CachedJwtProvider cachedJwtProvider) {
        super(client, cachedJwtProvider);
    }

    @JacocoGenerated
    public static ScrollClient defaultClient() {
        var cachedJwtProvider = getCachedJwtProvider(new SecretsReader());
        return new ScrollClient(HttpClient.newHttpClient(), cachedJwtProvider);
    }

    @Override
    public SwsResponse doSearch(ScrollQuery query)  {
        queryBuilderStart = query.getStartTime();
        return
            query.assemble()
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