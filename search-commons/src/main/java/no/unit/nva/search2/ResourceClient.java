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
import no.unit.nva.search2.common.OpenSearchClient;
import no.unit.nva.search2.common.QueryContentWrapper;
import no.unit.nva.search2.common.SwsResponse;
import nva.commons.core.JacocoGenerated;
import nva.commons.secrets.SecretsReader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ResourceClient extends OpenSearchClient<SwsResponse, ResourceQuery> {

    private static final Logger logger = LoggerFactory.getLogger(ResourceClient.class);

    private final CachedJwtProvider jwtProvider;
    private final HttpClient httpClient;
    private final BodyHandler<String> bodyHandler;
    private final UserSettingsClient userSettingsClient;

    public ResourceClient(CachedJwtProvider cachedJwtProvider, HttpClient client) {
        super();
        this.jwtProvider = cachedJwtProvider;
        this.httpClient = client;
        this.bodyHandler = HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8);
        this.userSettingsClient = new UserSettingsClient(cachedJwtProvider, client);
    }

    @JacocoGenerated
    public static ResourceClient defaultClient() {
        var cachedJwtProvider =
            OpenSearchClient.getCachedJwtProvider(new SecretsReader());

        return new ResourceClient(cachedJwtProvider, HttpClient.newHttpClient());
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


    @JacocoGenerated
    private HttpRequest createRequest(QueryContentWrapper qbs) {
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
    private SwsResponse handleResponse(HttpResponse<String> response) {
        if (response.statusCode() != HTTP_OK) {
            throw new RuntimeException(response.body());
        }
        return attempt(() -> singleLineObjectMapper.readValue(response.body(), SwsResponse.class))
            .map(result -> {
                logger.info("Opensearch Response Time: {} ms, TotalSize: {}", result.took(), result.getTotalSize());
                return result;
            }).orElseThrow();
    }

}
