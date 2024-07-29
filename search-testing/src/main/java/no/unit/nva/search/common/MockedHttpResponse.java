package no.unit.nva.search.common;

import static java.util.Objects.nonNull;
import static no.unit.nva.search.common.constant.Words.CONTENT_TYPE;
import static no.unit.nva.testutils.TestHeaders.APPLICATION_JSON;
import static nva.commons.core.ioutils.IoUtils.stringFromResources;
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

public class MockedHttpResponse {

    public static CompletableFuture<HttpResponse<Object>> mockedFutureHttpResponse(Path path) {
        return CompletableFuture.completedFuture(mockedHttpResponse(path));
    }

    public static CompletableFuture<HttpResponse<Object>> mockedFutureHttpResponse(String body) {
        return CompletableFuture.completedFuture(mockedHttpResponse(body));
    }

    public static CompletableFuture<HttpResponse<Object>> mockedFutureFailed() {
        return CompletableFuture.failedFuture(new Throwable("fututre failed"));
    }

    public static HttpResponse<Object> mockedHttpResponse(Path path) {
        return mockedHttpResponse(stringFromResources(path));
    }

    public static HttpResponse<Object> mockedHttpResponse(String body) {
        return mockedHttpResponse(body, nonNull(body) ? 200 : 400);
    }

    public static HttpResponse<Object> mockedHttpResponse(String body, int statusCode) {
        return new HttpResponse<>() {
            @Override
            public int statusCode() {
                return statusCode;
            }

            @Override
            public HttpRequest request() {
                return null;
            }

            @Override
            public Optional<HttpResponse<Object>> previousResponse() {
                return Optional.empty();
            }

            @Override
            public HttpHeaders headers() {
                return HttpHeaders.of(
                    Map.of(CONTENT_TYPE, Collections.singletonList(APPLICATION_JSON)),
                    (s, s2) -> true);
            }

            @Override
            public String body() {
                return body;
            }

            @Override
            public Optional<SSLSession> sslSession() {
                return Optional.empty();
            }

            @Override
            public URI uri() {
                return null;
            }

            @Override
            public HttpClient.Version version() {
                return null;
            }
        };
    }
}
