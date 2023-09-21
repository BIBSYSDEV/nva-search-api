package no.unit.nva.search2.sws;

import no.unit.nva.search.CachedJwtProvider;
import no.unit.nva.search.CognitoAuthenticator;
import no.unit.nva.search2.model.OpenSearchClient;
import no.unit.nva.search2.model.OpenSearchSwsResponse;
import nva.commons.apigateway.exceptions.BadGatewayException;
import nva.commons.core.JacocoGenerated;
import nva.commons.secrets.SecretsReader;
import org.apache.http.HttpStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpResponse.BodyHandler;
import java.nio.charset.StandardCharsets;

import static no.unit.nva.auth.AuthorizedBackendClient.AUTHORIZATION_HEADER;
import static no.unit.nva.auth.uriretriever.AuthorizedBackendUriRetriever.ACCEPT;
import static no.unit.nva.search2.constant.ApplicationConstants.objectMapperWithEmpty;
import static nva.commons.core.attempt.Try.attempt;

public class OpenSearchSwsClient implements OpenSearchClient<OpenSearchSwsResponse, ResourceQuery> {

    private static final Logger logger = LoggerFactory.getLogger(OpenSearchSwsClient.class);
    private static final String REQUESTING_SEARCH_FROM = "OpenSearchSwsClient url -> {}";
    private final CachedJwtProvider jwtProvider;
    private final HttpClient httpClient;
    private final BodyHandler<String> bodyHandler;

    public OpenSearchSwsClient(CachedJwtProvider jwtProvider, HttpClient client) {
        this.bodyHandler = HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8);
        this.jwtProvider = jwtProvider;
        this.httpClient = client;
    }

    @JacocoGenerated
    public static OpenSearchSwsClient defaultClient() {
        var cachedJwtProvider =
            OpenSearchClient.getUsernamePasswordStream(new SecretsReader())
                .map(OpenSearchClient::getCognitoCredentials)
                .map(CognitoAuthenticator::prepareWithCognitoCredentials)
                .map(CachedJwtProvider::prepareWithAuthenticator)
                .findFirst().orElseThrow();
        return new OpenSearchSwsClient(cachedJwtProvider, HttpClient.newHttpClient());
    }

    @Override
    public OpenSearchSwsResponse doSearch(ResourceQuery query, String mediaType) {
        var requestUri = query.openSearchUri();
        var httpRequest = getHttpRequest(requestUri, mediaType);
        return
            attempt(() -> httpClient.send(httpRequest, bodyHandler))
                .map(this::handleResponse)
                .orElseThrow();
    }

    private OpenSearchSwsResponse handleResponse(HttpResponse<String> response) throws BadGatewayException {
        if (response.statusCode() != HttpStatus.SC_OK) {
            logger.error(response.body());
            throw new BadGatewayException(response.body());
        }
        return getResponseBodyAs(response, OpenSearchSwsResponse.class);
    }

    private HttpRequest getHttpRequest(URI requestUri, String mediaType) {
        logger.info(REQUESTING_SEARCH_FROM, requestUri);
        return HttpRequest
                   .newBuilder(requestUri)
                   .headers(ACCEPT, mediaType, AUTHORIZATION_HEADER, jwtProvider.getValue().getToken())
                   .GET().build();
    }

    private static <T> T getResponseBodyAs(HttpResponse<String> response, Class<T> valueType) {
        return attempt(() -> objectMapperWithEmpty.readValue(response.body(), valueType)).orElseThrow();
    }
}
