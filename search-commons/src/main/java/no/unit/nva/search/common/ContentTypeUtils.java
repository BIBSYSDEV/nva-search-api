package no.unit.nva.search.common;

import static java.util.Objects.nonNull;
import static no.unit.nva.search.resource.Constants.V_2024_12_01_SIMPLER_MODEL;
import static no.unit.nva.search.resource.Constants.V_LEGACY;
import static org.apache.http.HttpHeaders.ACCEPT;

import java.util.Optional;
import java.util.function.Function;
import nva.commons.apigateway.RequestInfo;
import org.apache.http.entity.ContentType;

public class ContentTypeUtils {

  private static final String VERSION = "version";

  private ContentTypeUtils() {
    // NOOP
  }

  public static String negotiateVersion(String requestedVersion) {
    return V_2024_12_01_SIMPLER_MODEL.equals(requestedVersion) ? requestedVersion : V_LEGACY;
  }

  private static <T> T extractFromRequestInfo(RequestInfo requestInfo, Function<String, T> mapper) {
    return Optional.of(requestInfo)
        .map(RequestInfo::getHeaders)
        .map(map -> map.get(ACCEPT))
        .map(mapper)
        .orElse(null);
  }

  /** Extract the version field value if present from a header or else null. */
  private static String extractVersion(String headerValue) {
    var contentType = ContentType.parse(headerValue);
    return contentType.getParameter(VERSION);
  }

  /** Extract mimetype field value if present from a header or else null. */
  private static ContentType extractContentType(String headerValue) {
    return ContentType.parse(headerValue);
  }

  public static String extractVersionFromRequestInfo(RequestInfo requestInfo) {
    return extractFromRequestInfo(requestInfo, ContentTypeUtils::extractVersion);
  }

  public static ContentType extractContentTypeFromRequestInfo(RequestInfo requestInfo) {
    return extractFromRequestInfo(requestInfo, ContentTypeUtils::extractContentType);
  }

  public static String extractAcceptFromRequestInfo(RequestInfo requestInfo) {
    return extractFromRequestInfo(requestInfo, Function.identity());
  }

  public static String buildContentType(String acceptHeader, String version) {
    if (isTextCsvRequested(acceptHeader)) {
      return SupportedMediaType.TEXT_CSV.toHeaderValue(null);
    }

    var effectiveVersion = negotiateVersion(version);

    if (isAcceptHeaderPresent(acceptHeader, SupportedMediaType.APPLICATION_LD_JSON.getMimeType())) {
      return SupportedMediaType.APPLICATION_LD_JSON.toHeaderValue(effectiveVersion);
    }

    return SupportedMediaType.APPLICATION_JSON.toHeaderValue(effectiveVersion);
  }

  private static boolean isTextCsvRequested(String acceptHeader) {
    return nonNull(acceptHeader)
        && acceptHeader.contains(SupportedMediaType.TEXT_CSV.getMimeType());
  }

  private static boolean isAcceptHeaderPresent(String acceptHeader, String mimeType) {
    return nonNull(acceptHeader) && acceptHeader.contains(mimeType);
  }
}
