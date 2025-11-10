package no.unit.nva.search.common;

import java.nio.charset.StandardCharsets;
import org.apache.http.entity.ContentType;

public enum SupportedMediaType {
  TEXT_CSV("text/csv"),
  APPLICATION_JSON("application/json"),
  APPLICATION_LD_JSON("application/ld+json");

  private final ContentType contentType;

  private static final String CONTENT_TYPE_TEMPLATE = "%s; charset=utf-8";
  private static final String VERSIONED_CONTENT_TYPE_TEMPLATE = "%s; charset=utf-8; version=%s";

  SupportedMediaType(String mimeType) {
    this.contentType = ContentType.create(mimeType, StandardCharsets.UTF_8);
  }

  public String getMimeType() {
    return contentType.getMimeType();
  }

  public String toHeaderValue(String version) {
    if (this == TEXT_CSV) {
      return formatContentType(getMimeType());
    }
    return formatVersionedContentType(getMimeType(), version);
  }

  private static String formatContentType(String mimeType) {
    return String.format(CONTENT_TYPE_TEMPLATE, mimeType);
  }

  private static String formatVersionedContentType(String mimeType, String version) {
    return String.format(VERSIONED_CONTENT_TYPE_TEMPLATE, mimeType, version);
  }
}
