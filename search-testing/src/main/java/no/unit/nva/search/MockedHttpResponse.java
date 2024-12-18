package no.unit.nva.search;

import static no.unit.nva.search.model.constant.Words.CONTENT_TYPE;
import static no.unit.nva.testutils.TestHeaders.APPLICATION_JSON;

import static nva.commons.core.ioutils.IoUtils.stringFromResources;

import static java.util.Objects.nonNull;

import nva.commons.core.JacocoGenerated;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpHeaders;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Path;
import java.util.Collections;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

import javax.net.ssl.SSLSession;

@JacocoGenerated
public class MockedHttpResponse {

    public static CompletableFuture<HttpResponse<Object>> mockedFutureHttpResponse(Path path) {
        return CompletableFuture.completedFuture(mockedHttpResponse(path));
    }

    public static CompletableFuture<HttpResponse<Object>> mockedFutureHttpResponse(String body) {
        return CompletableFuture.completedFuture(mockedHttpResponse(body));
    }

    public static CompletableFuture<HttpResponse<Object>> mockedFutureFailed() {
        return CompletableFuture.failedFuture(new Throwable("future failed"));
    }

    public static HttpResponse<Object> mockedHttpResponse(Path path) {
        return mockedHttpResponse(stringFromResources(path));
    }

    public static HttpResponse<Object> mockedHttpResponse(String body) {
        return mockedHttpResponse(body, nonNull(body) ? 200 : 400);
    }

    public static HttpResponse<Object> mockedHttpResponse(String body, int statusCode) {
        return new HttpResponse<>() {
            @JacocoGenerated
            @Override
            public int statusCode() {
                return statusCode;
            }

            @JacocoGenerated
            @Override
            public HttpRequest request() {
                return null;
            }

            @JacocoGenerated
            @Override
            public Optional<HttpResponse<Object>> previousResponse() {
                return Optional.empty();
            }

            @JacocoGenerated
            @Override
            public HttpHeaders headers() {
                return HttpHeaders.of(
                        Map.of(CONTENT_TYPE, Collections.singletonList(APPLICATION_JSON)),
                        (s, s2) -> true);
            }

            @JacocoGenerated
            @Override
            public String body() {
                return body;
            }

            @JacocoGenerated
            @Override
            public Optional<SSLSession> sslSession() {
                return Optional.empty();
            }

            @JacocoGenerated
            @Override
            public URI uri() {
                return null;
            }

            @JacocoGenerated
            @Override
            public HttpClient.Version version() {
                return null;
            }
        };
    }
}
