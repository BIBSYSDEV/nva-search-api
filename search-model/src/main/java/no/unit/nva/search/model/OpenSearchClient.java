package no.unit.nva.search.model;

import static no.unit.nva.search.model.constant.Words.AMPERSAND;
import static no.unit.nva.search.model.constant.Words.CONTENT_TYPE;

import static java.net.HttpURLConnection.HTTP_OK;
import static java.util.stream.Collectors.joining;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.google.common.net.MediaType;

import no.unit.nva.auth.AuthorizedBackendClient;
import no.unit.nva.auth.uriretriever.UriRetriever;
import no.unit.nva.commons.json.JsonSerializable;
import no.unit.nva.search.model.jwt.CachedJwtProvider;
import no.unit.nva.search.model.records.QueryContentWrapper;
import no.unit.nva.search.model.records.ResponseLogInfo;
import no.unit.nva.search.model.records.SwsResponse;

import nva.commons.core.attempt.FunctionWithException;
import nva.commons.core.attempt.Try;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpResponse.BodyHandler;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.function.BinaryOperator;

/**
 * Abstract class for OpenSearch clients.
 *
 * @author Stig Norland
 * @param <R> the type of the response object. The response object is the result of the search
 *     query.
 * @param <Q> the type of the query object. The query object is the object that contains the
 *     parameters for the search query.
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
        if (query.filters().hasContent()) {
            logger.info(query.filters().toString());
        }
        queryBuilderStart = query.getStartTime();
        queryParameters =
                query.parameters().asMap().entrySet().stream()
                        .map(Object::toString)
                        .collect(joining(AMPERSAND));

        var completableFutures =
                query.assemble()
                        .map(this::createRequest)
                        .map(this::fetch)
                        .map(this::handleResponse)
                        .toList();

        return combineAndReturn(completableFutures);
    }

    protected CompletableFuture<R> handleResponse(
            CompletableFuture<HttpResponse<String>> completableFuture) {
        return completableFuture.thenApplyAsync(
                response -> {
                    if (response.statusCode() != HTTP_OK) {
                        throw new RuntimeException(response.body());
                    }
                    return Try.attempt(() -> jsonToResponse(response))
                            .map(logAndReturnResult())
                            .orElseThrow();
                });
    }

    protected abstract R jsonToResponse(HttpResponse<String> response)
            throws JsonProcessingException;

    protected abstract BinaryOperator<R> responseAccumulator();

    protected abstract FunctionWithException<R, R, RuntimeException> logAndReturnResult();

    protected R combineAndReturn(List<CompletableFuture<R>> completableFutures) {
        return completableFutures.size() == 2
                ? completableFutures
                        .get(0)
                        .thenCombineAsync(completableFutures.get(1), responseAccumulator())
                        .join()
                : completableFutures.get(0).join();
    }

    protected CompletableFuture<HttpResponse<String>> fetch(HttpRequest request) {
        var fetchStart = Instant.now();
        return httpClient
                .sendAsync(request, bodyHandler)
                .thenApplyAsync(
                        response -> {
                            fetchDuration = Duration.between(fetchStart, Instant.now()).toMillis();
                            return response;
                        })
                .exceptionallyAsync(
                        responseFailure -> {
                            fetchDuration = Duration.between(fetchStart, Instant.now()).toMillis();
                            var error =
                                    new ErrorEntry(
                                                    request.uri(),
                                                    responseFailure.getCause().getMessage())
                                            .toJsonString();
                            logger.error(error);
                            return null;
                        });
    }

    protected HttpRequest createRequest(QueryContentWrapper qbs) {
        logger.debug(qbs.body());
        return HttpRequest.newBuilder(qbs.uri())
                .headers(
                        UriRetriever.ACCEPT,
                        MediaType.JSON_UTF_8.toString(),
                        CONTENT_TYPE,
                        MediaType.JSON_UTF_8.toString(),
                        AuthorizedBackendClient.AUTHORIZATION_HEADER,
                        jwtProvider.getValue().getToken())
                .POST(HttpRequest.BodyPublishers.ofString(qbs.body()))
                .build();
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
        return Duration.between(queryBuilderStart, Instant.now()).toMillis();
    }

    protected record ErrorEntry(URI requestUri, String exception) implements JsonSerializable {}
}
