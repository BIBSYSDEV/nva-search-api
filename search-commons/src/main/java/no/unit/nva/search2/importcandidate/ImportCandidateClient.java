package no.unit.nva.search2.importcandidate;

import static java.net.HttpURLConnection.HTTP_OK;
import static no.unit.nva.commons.json.JsonUtils.singleLineObjectMapper;
import static nva.commons.core.attempt.Try.attempt;
import java.net.http.HttpClient;
import java.net.http.HttpResponse;

import no.unit.nva.search2.common.jwt.CachedJwtProvider;
import no.unit.nva.search2.common.OpenSearchClient;
import no.unit.nva.search2.common.records.SwsResponse;
import nva.commons.core.JacocoGenerated;
import nva.commons.secrets.SecretsReader;

public class ImportCandidateClient extends OpenSearchClient<SwsResponse, ImportCandidateSearchQuery> {

    public ImportCandidateClient(HttpClient client, CachedJwtProvider cachedJwtProvider) {
        super(client, cachedJwtProvider);
    }

    @JacocoGenerated
    public static ImportCandidateClient defaultClient() {
        var cachedJwtProvider = OpenSearchClient.getCachedJwtProvider(new SecretsReader());
        return new ImportCandidateClient(HttpClient.newHttpClient(), cachedJwtProvider);
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
