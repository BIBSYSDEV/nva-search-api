package no.unit.nva.search.common;

import static java.net.HttpURLConnection.HTTP_OK;
import static java.util.stream.Collectors.joining;
import static no.unit.nva.auth.AuthorizedBackendClient.AUTHORIZATION_HEADER;
import static no.unit.nva.auth.AuthorizedBackendClient.CONTENT_TYPE;
import static no.unit.nva.auth.uriretriever.UriRetriever.ACCEPT;
import static no.unit.nva.search.common.constant.Words.AMPERSAND;
import static nva.commons.core.attempt.Try.attempt;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.google.common.net.MediaType;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse.BodyHandler;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.function.BinaryOperator;
import no.unit.nva.commons.json.JsonSerializable;
import no.unit.nva.search.common.jwt.CachedJwtProvider;
import no.unit.nva.search.common.records.QueryContentWrapper;
import no.unit.nva.search.common.records.ResponseLogInfo;
import no.unit.nva.search.common.records.SwsResponse;
import nva.commons.core.JacocoGenerated;
import nva.commons.core.attempt.FunctionWithException;
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

    public R doSearch(Q query) {
        queryBuilderStart = query.getStartTime();
        queryParameters = query.parameters().asMap()
            .entrySet().stream()
            .map(Object::toString)
            .collect(joining(AMPERSAND));

        var completableFutures = query.assemble()
            .map(this::createRequest)
            .map(this::fetch)
            .map(this::handleResponse).toList();

        return combineAndReturn(completableFutures);
    }


    protected CompletableFuture<R> handleResponse(CompletableFuture<HttpResponse<String>> completableFuture) {
        return completableFuture
            .thenApplyAsync(response -> {
                if (response.statusCode() != HTTP_OK) {
                    throw new RuntimeException(response.body());
                }
                return attempt(() -> jsonToResponse(response))
                    .map(logAndReturnResult())
                    .orElseThrow();
            });
    }

    protected abstract R jsonToResponse(HttpResponse<String> response) throws JsonProcessingException;

    @JacocoGenerated
    protected BinaryOperator<R> responseAccumulator(){
        return (a, b) -> a;
    }

    protected abstract FunctionWithException<R, R, RuntimeException> logAndReturnResult();

    private R combineAndReturn(List<CompletableFuture<R>> completableFutures) {
        return completableFutures.size() == 2
            ? completableFutures.get(0).thenCombineAsync(completableFutures.get(1), responseAccumulator()).join()
            : completableFutures.get(0).join();
    }


    protected CompletableFuture<HttpResponse<String>> fetch(HttpRequest request) {
        var fetchStart = Instant.now();
        return httpClient.sendAsync(request, bodyHandler)
            .thenApplyAsync(response -> {
                fetchDuration = Duration.between(fetchStart, Instant.now()).toMillis();
                return response;
            })
            .exceptionallyAsync(responseFailure -> {
                fetchDuration = Duration.between(fetchStart, Instant.now()).toMillis();
                var error = new ErrorEntry(request.uri(), responseFailure.getCause().getMessage()).toJsonString();
                logger.error(error);
                return null;
            });
    }

    protected HttpRequest createRequest(QueryContentWrapper qbs) {
        logger.debug(qbs.body());
        return HttpRequest
                   .newBuilder(qbs.uri())
                   .headers(
                       ACCEPT, MediaType.JSON_UTF_8.toString(),
                       CONTENT_TYPE, MediaType.JSON_UTF_8.toString(),
                       AUTHORIZATION_HEADER, jwtProvider.getValue().getToken())
                   .POST(HttpRequest.BodyPublishers.ofString(qbs.body())).build();
    }

    protected String buildLogInfo(SwsResponse result) {
        return ResponseLogInfo.builder()
            .withTotalTime(totalDuration())
            .withFetchTime(fetchDuration)
            .withSwsResponse(result)
            .withSearchQuery(queryParameters)
            .toJsonString();
    }

    protected long totalDuration() {
        return Duration
            .between(queryBuilderStart, Instant.now())
            .toMillis();
    }

    protected record ErrorEntry(URI requestUri, String exception) implements JsonSerializable {

    }

}