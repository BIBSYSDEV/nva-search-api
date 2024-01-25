package no.unit.nva.search2.common;

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
import javax.net.ssl.SSLSession;
import org.jetbrains.annotations.NotNull;

public class MockedHttpResponse {
    
    @NotNull
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
                return HttpHeaders.of(Map.of("Content-Type", Collections.singletonList("application/json")),
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
                return HttpHeaders.of(Map.of("Content-Type", Collections.singletonList("application/json")),
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
