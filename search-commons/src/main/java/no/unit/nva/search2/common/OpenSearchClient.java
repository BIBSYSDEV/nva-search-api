package no.unit.nva.search2.common;

import static java.util.stream.Collectors.joining;
import static no.unit.nva.auth.AuthorizedBackendClient.AUTHORIZATION_HEADER;
import static no.unit.nva.auth.AuthorizedBackendClient.CONTENT_TYPE;
import static no.unit.nva.auth.uriretriever.UriRetriever.ACCEPT;
import static no.unit.nva.search2.common.constant.Functions.readSearchInfrastructureAuthUri;
import static no.unit.nva.search2.common.constant.Words.AMPERSAND;
import static no.unit.nva.search2.common.constant.Words.SEARCH_INFRASTRUCTURE_CREDENTIALS;
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
import java.util.concurrent.CompletableFuture;
import java.util.stream.Stream;
import no.unit.nva.auth.CognitoCredentials;
import no.unit.nva.commons.json.JsonSerializable;
import no.unit.nva.search2.common.jwt.CachedJwtProvider;
import no.unit.nva.search2.common.jwt.CognitoAuthenticator;
import no.unit.nva.search2.common.records.UsernamePasswordWrapper;
import no.unit.nva.search2.common.records.QueryContentWrapper;
import no.unit.nva.search2.common.records.ResponseLogInfo;
import no.unit.nva.search2.common.records.SwsResponse;
import nva.commons.core.JacocoGenerated;
import nva.commons.core.attempt.FunctionWithException;
import nva.commons.secrets.SecretsReader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Stig Norland
 */
public abstract class OpenSearchClient<R, Q extends Query<?>> {

    protected static final Logger logger = LoggerFactory.getLogger(OpenSearchClient.class);

    protected final HttpClient httpClient;
    protected final BodyHandler<String> bodyHandler;
    protected final CachedJwtProvider jwtProvider;
    protected Instant queryBuilderStart;
    protected long fetchDuration;
    protected String queryParameters;

    public OpenSearchClient(HttpClient httpClient, CachedJwtProvider jwtProvider) {
        this.bodyHandler = HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8);
        this.httpClient = httpClient;
        this.jwtProvider = jwtProvider;
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

    public R doSearch(Q query) {
        queryBuilderStart = query.getStartTime();
        queryParameters = query.parameters().asMap()
            .entrySet().stream()
            .map(Object::toString)
            .collect(joining(AMPERSAND));
        return
            query.assemble()
                .map(this::createRequest)
                .map(this::fetch)
                .map(this::handleResponse)
                .findFirst().orElseThrow();
    }

    protected abstract R handleResponse(HttpResponse<String> response);



    protected Stream<AsyncHttpResponse> fetch(AsyncHttpReqest async) {
//            var fetchStart = Instant.now();
        return httpClient.sendAsync(async.request, bodyHandler).thenApply(response ->new AsyncHttpResponse(response.body(),async.requestLeft())
//            .map(response -> {
//                fetchDuration = Duration.between(fetchStart, Instant.now()).toMillis();
//                return response;
//            })
//            .orElse(responseFailure -> {
//                fetchDuration = Duration.between(fetchStart, Instant.now()).toMillis();
//                logger.error(new ErrorEntry(async.uri(), responseFailure.getException()).toJsonString());
//                return null;
//            });
    }

    protected Stream<AsyncHttpReqest> createRequest(QueryContentWrapper qbs) {
        logger.debug(qbs.body());
        var requests = hasAggregation(qbs) ? 2 : 1;
        for(int i = requests; i > 0; i--) {
            var request = HttpRequest
                .newBuilder(qbs.uri())
                .headers(
                    ACCEPT, MediaType.JSON_UTF_8.toString(),
                    CONTENT_TYPE, MediaType.JSON_UTF_8.toString(),
                    AUTHORIZATION_HEADER, jwtProvider.getValue().getToken())
                .POST(HttpRequest.BodyPublishers.ofString(qbs.body())).build());
            return Stream.of(new AsyncHttpReqest(request,i));
        }
    }

    private boolean hasAggregation(QueryContentWrapper qbs) {
        return false;
    }


    protected FunctionWithException<SwsResponse, SwsResponse, RuntimeException> logAndReturnResult() {
        return result -> {
            logger.info(ResponseLogInfo.builder()
                .withTotalTime(totalDuration())
                .withFetchTime(fetchDuration)
                .withSwsResponse(result)
                .withSearchQuery(queryParameters)
                .toJsonString()
            );
            return result;
        };
    }


    private long totalDuration() {
        return Duration
            .between(queryBuilderStart, Instant.now())
            .toMillis();
    }

    record ErrorEntry(URI requestUri, Exception exception) implements JsonSerializable {

    }

    record AsyncHttpReqest(HttpRequest request, Integer requestLeft){
    }

    record AsyncHttpResponse(CompletableFuture<String> body, Integer responseLeft){
    }
}