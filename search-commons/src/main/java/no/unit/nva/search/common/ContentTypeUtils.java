package no.unit.nva.search.common;

import static org.apache.http.HttpHeaders.ACCEPT;

import nva.commons.apigateway.RequestInfo;
import nva.commons.core.JacocoGenerated;

import org.apache.http.entity.ContentType;

import java.util.Optional;

public final class ContentTypeUtils {

    public static final String VERSION = "version";

    @JacocoGenerated
    private ContentTypeUtils() {}

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
        return Optional.of(requestInfo)
                .map(RequestInfo::getHeaders)
                .map(map -> map.get(ACCEPT))
                .map(ContentTypeUtils::extractVersion)
                .orElse(null);
    }

    public static ContentType extractContentTypeFromRequestInfo(RequestInfo requestInfo) {
        return Optional.of(requestInfo)
                .map(RequestInfo::getHeaders)
                .map(map -> map.get(ACCEPT))
                .map(ContentTypeUtils::extractContentType)
                .orElse(null);
    }
}
