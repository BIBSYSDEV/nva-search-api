package no.unit.nva.search2;

import static java.net.HttpURLConnection.HTTP_OK;
import static no.unit.nva.auth.AuthorizedBackendClient.AUTHORIZATION_HEADER;
import static no.unit.nva.auth.AuthorizedBackendClient.CONTENT_TYPE;
import static no.unit.nva.commons.json.JsonUtils.singleLineObjectMapper;
import static no.unit.nva.search.utils.UriRetriever.ACCEPT;
import static nva.commons.core.attempt.Try.attempt;
import com.google.common.net.MediaType;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpResponse.BodyHandler;
import java.nio.charset.StandardCharsets;
import no.unit.nva.search.CachedJwtProvider;
import no.unit.nva.search2.model.OpenSearchClient;
import no.unit.nva.search2.model.OpenSearchSwsResponse;
import no.unit.nva.search2.model.QueryBuilderSourceWrapper;
import nva.commons.core.JacocoGenerated;
import nva.commons.secrets.SecretsReader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ImportCandidateClient implements OpenSearchClient<OpenSearchSwsResponse, ImportCandidateQuery> {

    private static final Logger logger = LoggerFactory.getLogger(ImportCandidateClient.class);
    
    private final CachedJwtProvider jwtProvider;
    private final HttpClient httpClient;
    private final BodyHandler<String> bodyHandler;

    public ImportCandidateClient(CachedJwtProvider cachedJwtProvider, HttpClient client) {
        super();
        this.jwtProvider = cachedJwtProvider;
        this.httpClient = client;
        this.bodyHandler = HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8);
    }
    
    @JacocoGenerated
    public static ImportCandidateClient defaultClient() {
        var cachedJwtProvider =
            OpenSearchClient.getCachedJwtProvider(new SecretsReader());

        return new ImportCandidateClient(cachedJwtProvider, HttpClient.newHttpClient());
    }
    
    @Override
    public OpenSearchSwsResponse doSearch(ImportCandidateQuery query) {
        return
            query.createQueryBuilderStream()
                .map(this::createRequest)
                .map(this::fetch)
                .map(this::handleResponse)
                .findFirst().orElseThrow();
    }
    
    @JacocoGenerated
    private HttpRequest createRequest(QueryBuilderSourceWrapper qbs) {
        logger.info(qbs.source().query().toString());
        return HttpRequest
            .newBuilder(qbs.requestUri())
            .headers(
                ACCEPT, MediaType.JSON_UTF_8.toString(),
                CONTENT_TYPE, MediaType.JSON_UTF_8.toString(),
                AUTHORIZATION_HEADER, jwtProvider.getValue().getToken())
            .POST(HttpRequest.BodyPublishers.ofString(qbs.source().toString())).build();
    }
    
    private HttpResponse<String> fetch(HttpRequest httpRequest) {
        return attempt(() -> httpClient.send(httpRequest, bodyHandler)).orElseThrow();
    }
    
    @JacocoGenerated
    private OpenSearchSwsResponse handleResponse(HttpResponse<String> response) {
        if (response.statusCode() != HTTP_OK) {
            throw new RuntimeException(response.body());
        }
        return attempt(() -> singleLineObjectMapper.readValue(response.body(), OpenSearchSwsResponse.class))
            .orElseThrow();
    }
}
