package no.unit.nva.search.common;

import static org.apache.http.HttpHeaders.ACCEPT;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;

import java.net.URI;
import java.net.http.HttpClient;
import java.util.Map;
import java.util.function.Supplier;
import nva.commons.apigateway.RequestInfo;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class ContentTypeUtilsTest {
  public static final String ACCEPT_HEADER_VALUE = "application/json; version=2023-05-10";
  public static final String ACCEPT_HEADER_VALUE_WITH_QUOTES =
      "application/json; version=\"2023-05-10\"";
  public static final String ACCEPT_HEADER_VALUE_WITHOUT_VERSION = "application/json";
  public static final String VERSION_VALUE = "2023-05-10";
  public static final String MIME_TYPE = "application/json";

  private static final Supplier<URI> URI_SUPPLIER = () -> URI.create("");
  private RequestInfo requestInfo;

  @BeforeEach
  void setup() {
    requestInfo = new RequestInfo(HttpClient.newHttpClient(), URI_SUPPLIER, URI_SUPPLIER);
  }

  @Test
  void asssertThatMimeTypeAndVersionIsExtractedWhenProvided() {
    requestInfo.setHeaders(Map.of(ACCEPT, ACCEPT_HEADER_VALUE));

    var version = ContentTypeUtils.extractVersionFromRequestInfo(requestInfo);
    var mimeType = ContentTypeUtils.extractContentTypeFromRequestInfo(requestInfo).getMimeType();

    assertThat(mimeType, equalTo(MIME_TYPE));
    assertThat(version, equalTo(VERSION_VALUE));
  }

  @Test
  void asssertThatMimeTypeAndVersionIsExtractedWhenProvidedAndVersionHasQuotes() {
    requestInfo.setHeaders(Map.of(ACCEPT, ACCEPT_HEADER_VALUE_WITH_QUOTES));

    var version = ContentTypeUtils.extractVersionFromRequestInfo(requestInfo);
    var mimeType = ContentTypeUtils.extractContentTypeFromRequestInfo(requestInfo).getMimeType();

    assertThat(mimeType, equalTo(MIME_TYPE));
    assertThat(version, equalTo(VERSION_VALUE));
  }

  @Test
  void asssertThatMimeTypeAndVersionAreNullWhenNotProvided() {
    requestInfo.setHeaders(Map.of());

    var version = ContentTypeUtils.extractVersionFromRequestInfo(requestInfo);
    var mimeType = ContentTypeUtils.extractContentTypeFromRequestInfo(requestInfo);

    assertThat(mimeType, equalTo(null));
    assertThat(version, equalTo(null));
  }
}
