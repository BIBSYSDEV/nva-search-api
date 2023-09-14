package no.unit.nva.search2.common;

import java.net.http.HttpClient;
import java.net.http.HttpResponse.BodyHandler;
import java.util.stream.Stream;
import no.unit.nva.auth.CognitoCredentials;
import no.unit.nva.search.CachedJwtProvider;
import no.unit.nva.search.CognitoAuthenticator;
import no.unit.nva.search.models.UsernamePasswordWrapper;
import no.unit.nva.search2.model.OpenSearchSwsResponse;
import no.unit.nva.search2.model.Problem;
import nva.commons.apigateway.exceptions.BadGatewayException;
import nva.commons.core.JacocoGenerated;
import nva.commons.secrets.SecretsReader;
import org.apache.http.HttpStatus;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;

import static no.unit.nva.auth.AuthorizedBackendClient.AUTHORIZATION_HEADER;
import static no.unit.nva.auth.uriretriever.AuthorizedBackendUriRetriever.ACCEPT;
import static no.unit.nva.search.RestHighLevelClientWrapper.SEARCH_INFRASTRUCTURE_CREDENTIALS;
import static no.unit.nva.search2.constant.ApplicationConstants.objectMapperWithEmpty;
import static no.unit.nva.search2.constant.ApplicationConstants.readSearchInfrastructureAuthUri;
import static nva.commons.core.attempt.Try.attempt;

public class OpenSearchSwsClient {

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
    public static OpenSearchSwsClient defaultSwsClient() {
        var cachedJwtProvider =
            getUsernamePasswordStream(new SecretsReader())
                .map(OpenSearchSwsClient::getCognitoCredentials)
                .map(CognitoAuthenticator::prepareWithCognitoCredentials)
                .map(CachedJwtProvider::prepareWithAuthenticator)
                .findFirst().orElseThrow();
        return new OpenSearchSwsClient(cachedJwtProvider, HttpClient.newHttpClient());
    }

    public OpenSearchSwsResponse doSearch(URI requestUri, String mediaType)  {
        var httpRequest = getHttpRequest(requestUri, mediaType);
        return
            attempt(() -> httpClient.send(httpRequest, bodyHandler))
                .map(this::handleResponse)
                .orElseThrow();
    }

    private OpenSearchSwsResponse handleResponse(HttpResponse<String> response) throws BadGatewayException {
        if (response.statusCode() != HttpStatus.SC_OK) {
            var problem = getResponseBodyAs(response, Problem.class);
            logger.error(problem.message());
            throw new BadGatewayException(problem.message());
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

    @NotNull
    private static CognitoCredentials getCognitoCredentials(UsernamePasswordWrapper wrapper) {
        var uri = URI.create(readSearchInfrastructureAuthUri());
        return new CognitoCredentials(wrapper::getUsername, wrapper::getPassword, uri);
    }

    @NotNull
    private static Stream<UsernamePasswordWrapper> getUsernamePasswordStream(SecretsReader secretsReader) {
        return Stream.of(
            secretsReader.fetchClassSecret(SEARCH_INFRASTRUCTURE_CREDENTIALS, UsernamePasswordWrapper.class));
    }
}
