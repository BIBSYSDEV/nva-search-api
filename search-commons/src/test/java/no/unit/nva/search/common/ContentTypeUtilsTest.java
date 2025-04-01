package no.unit.nva.search.common;

import static org.apache.http.HttpHeaders.ACCEPT;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;

import com.fasterxml.jackson.core.JsonProcessingException;
import java.util.Map;
import no.unit.nva.commons.json.JsonUtils;
import no.unit.nva.testutils.HandlerRequestBuilder;
import nva.commons.apigateway.RequestInfo;
import nva.commons.apigateway.exceptions.ApiIoException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class ContentTypeUtilsTest {
  public static final String ACCEPT_HEADER_VALUE = "application/json; version=2023-05-10";
  public static final String ACCEPT_HEADER_VALUE_WITH_QUOTES =
      "application/json; version=\"2023-05-10\"";
  public static final String VERSION_VALUE = "2023-05-10";
  public static final String MIME_TYPE = "application/json";

  private RequestInfo requestInfo;

  @BeforeEach
  void setup() throws ApiIoException, JsonProcessingException {
    requestInfo =
        RequestInfo.fromRequest(new HandlerRequestBuilder<Void>(JsonUtils.dtoObjectMapper).build());
  }

  @Test
  void assertThatMimeTypeAndVersionIsExtractedWhenProvided() {
    requestInfo.setHeaders(Map.of(ACCEPT, ACCEPT_HEADER_VALUE));

    var version = ContentTypeUtils.extractVersionFromRequestInfo(requestInfo);
    var mimeType = ContentTypeUtils.extractContentTypeFromRequestInfo(requestInfo).getMimeType();

    assertThat(mimeType, equalTo(MIME_TYPE));
    assertThat(version, equalTo(VERSION_VALUE));
  }

  @Test
  void assertThatMimeTypeAndVersionIsExtractedWhenProvidedAndVersionHasQuotes() {
    requestInfo.setHeaders(Map.of(ACCEPT, ACCEPT_HEADER_VALUE_WITH_QUOTES));

    var version = ContentTypeUtils.extractVersionFromRequestInfo(requestInfo);
    var mimeType = ContentTypeUtils.extractContentTypeFromRequestInfo(requestInfo).getMimeType();

    assertThat(mimeType, equalTo(MIME_TYPE));
    assertThat(version, equalTo(VERSION_VALUE));
  }

  @Test
  void assertThatMimeTypeAndVersionAreNullWhenNotProvided() {
    requestInfo.setHeaders(Map.of());

    var version = ContentTypeUtils.extractVersionFromRequestInfo(requestInfo);
    var mimeType = ContentTypeUtils.extractContentTypeFromRequestInfo(requestInfo);

    assertThat(mimeType, equalTo(null));
    assertThat(version, equalTo(null));
  }
}
