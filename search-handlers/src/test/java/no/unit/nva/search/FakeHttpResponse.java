package no.unit.nva.search;

import java.net.URI;
import java.net.http.HttpClient.Version;
import java.net.http.HttpHeaders;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Optional;
import javax.net.ssl.SSLSession;

public class FakeHttpResponse implements HttpResponse<Object> {
  private final int statusCode;

  public FakeHttpResponse(int statusCode) {
    this.statusCode = statusCode;
  }

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
    return null;
  }

  @Override
  public Object body() {
    return "";
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
  public Version version() {
    return null;
  }
}
