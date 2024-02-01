package no.unit.nva.search2.common;

import static no.unit.nva.auth.AuthorizedBackendClient.AUTHORIZATION_HEADER;
import static no.unit.nva.auth.AuthorizedBackendClient.CONTENT_TYPE;
import static no.unit.nva.search.utils.UriRetriever.ACCEPT;
import static no.unit.nva.search2.constant.Functions.readSearchInfrastructureAuthUri;
import static no.unit.nva.search2.constant.Words.SEARCH_INFRASTRUCTURE_CREDENTIALS;
import static nva.commons.core.attempt.Try.attempt;
import com.google.common.net.MediaType;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpResponse.BodyHandler;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.stream.Stream;
import no.unit.nva.auth.CognitoCredentials;
import no.unit.nva.commons.json.JsonSerializable;
import no.unit.nva.search.CachedJwtProvider;
import no.unit.nva.search.CognitoAuthenticator;
import no.unit.nva.search.models.UsernamePasswordWrapper;
import no.unit.nva.search2.dto.ResponseLogInfo;
import nva.commons.core.JacocoGenerated;
import nva.commons.core.attempt.FunctionWithException;
import nva.commons.secrets.SecretsReader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class OpenSearchClient<R, Q extends Query<?>> {

    protected static final Logger logger = LoggerFactory.getLogger(OpenSearchClient.class);

    protected final HttpClient httpClient;
    protected final BodyHandler<String> bodyHandler;
    protected final CachedJwtProvider jwtProvider;
    protected Instant requestStart;

    public OpenSearchClient(HttpClient httpClient, CachedJwtProvider jwtProvider) {
        this.bodyHandler = HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8);
        this.httpClient = httpClient;
        this.jwtProvider = jwtProvider;
    }

    public abstract R doSearch(Q query);

    protected abstract R handleResponse(HttpResponse<String> response);

    protected HttpResponse<String> fetch(HttpRequest httpRequest) {
        return attempt(() -> httpClient.send(httpRequest, bodyHandler))
            .orElse(responseFailure -> {
                logger.error(
                    new ErrorEntry(httpRequest.uri(), responseFailure.getException()).toJsonString()
                );
                return null;
            });
    }

    protected HttpRequest createRequest(QueryContentWrapper qbs) {
        logger.debug(qbs.source().query().toString());
        requestStart = Instant.now();
        return HttpRequest
            .newBuilder(qbs.requestUri())
            .headers(
                ACCEPT, MediaType.JSON_UTF_8.toString(),
                CONTENT_TYPE, MediaType.JSON_UTF_8.toString(),
                AUTHORIZATION_HEADER, jwtProvider.getValue().getToken())
            .POST(HttpRequest.BodyPublishers.ofString(qbs.source().toString())).build();
    }

    @JacocoGenerated
    public static Stream<UsernamePasswordWrapper> getUsernamePasswordStream(SecretsReader secretsReader) {
        return Stream.of(
            secretsReader.fetchClassSecret(SEARCH_INFRASTRUCTURE_CREDENTIALS, UsernamePasswordWrapper.class));
    }

    @JacocoGenerated
    public static CognitoCredentials getCognitoCredentials(UsernamePasswordWrapper wrapper) {
        var uri = URI.create(readSearchInfrastructureAuthUri());
        return new CognitoCredentials(wrapper::getUsername, wrapper::getPassword, uri);
    }

    @JacocoGenerated
    public static CachedJwtProvider getCachedJwtProvider(SecretsReader reader) {
        return
            getUsernamePasswordStream(reader)
                .map(OpenSearchClient::getCognitoCredentials)
                .map(CognitoAuthenticator::prepareWithCognitoCredentials)
                .map(CachedJwtProvider::prepareWithAuthenticator)
                .findFirst().orElseThrow();
    }

    protected FunctionWithException<SwsResponse, SwsResponse, RuntimeException> logAndReturnResult() {
        return result -> {
            logger.info(
                ResponseLogInfo.builder()
                    .withResponseTime(getRequestDuration())
                    .withOpensearchResponseTime(result.took())
                    .withTotalHits(result.getTotalSize())
                    .toJsonString());
            return result;
        };
    }

    private long getRequestDuration() {
        return Duration.between(requestStart, Instant.now()).toMillis();
    }

    record ErrorEntry(URI requestUri, Exception exception) implements JsonSerializable {

    }
}
