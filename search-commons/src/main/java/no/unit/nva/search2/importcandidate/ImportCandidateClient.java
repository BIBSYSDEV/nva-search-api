package no.unit.nva.search2.importcandidate;

import java.net.http.HttpClient;
import java.net.http.HttpResponse;

import no.unit.nva.search.CachedJwtProvider;
import no.unit.nva.search2.common.OpenSearchClient;
import no.unit.nva.search2.common.records.SwsResponse;
import nva.commons.core.JacocoGenerated;
import nva.commons.secrets.SecretsReader;

import static java.net.HttpURLConnection.HTTP_OK;
import static no.unit.nva.commons.json.JsonUtils.singleLineObjectMapper;
import static nva.commons.core.attempt.Try.attempt;

public class ImportCandidateClient extends OpenSearchClient<SwsResponse, ImportCandidateQuery> {

    @JacocoGenerated
    public static ImportCandidateClient defaultClient() {
        var cachedJwtProvider = OpenSearchClient.getCachedJwtProvider(new SecretsReader());
        return new ImportCandidateClient(HttpClient.newHttpClient(), cachedJwtProvider);
    }

    public ImportCandidateClient(HttpClient client, CachedJwtProvider cachedJwtProvider) {
        super(client, cachedJwtProvider);
    }

    @Override
    public SwsResponse doSearch(ImportCandidateQuery query) {
        return
            query.createQueryBuilderStream()
                .map(this::createRequest)
                .map(this::fetch)
                .map(this::handleResponse)
                .findFirst().orElseThrow();
    }


    protected SwsResponse handleResponse(HttpResponse<String> response) {
        if (response.statusCode() != HTTP_OK) {
            throw new RuntimeException(response.body());
        }
        return attempt(() -> singleLineObjectMapper.readValue(response.body(), SwsResponse.class))
            .map(logAndReturnResult())
            .orElseThrow();
    }
}
