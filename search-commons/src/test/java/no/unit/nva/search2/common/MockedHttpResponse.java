package no.unit.nva.search2.common;

import static no.unit.nva.search2.common.constant.Words.CONTENT_TYPE;
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

    public static CompletableFuture<HttpResponse<Object>> mockedFutureHttpResponse(String filename) {
        return CompletableFuture.completedFuture(mockedHttpResponse(filename));
    }


    public static HttpResponse<Object> mockedHttpResponse(String filename) {
        return new HttpResponse<>() {
            @Override
            public int statusCode() {
                return 200;
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
                return stringFromResources(Path.of(filename));
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

    public static HttpResponse<Object> mockedHttpResponse(String filename, int statusCode) {
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
                return stringFromResources(Path.of(filename));
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
