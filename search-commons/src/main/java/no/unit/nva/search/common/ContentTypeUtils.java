package no.unit.nva.search.common;

import nva.commons.apigateway.RequestInfo;

import org.apache.http.entity.ContentType;

import java.util.Optional;

public class ContentTypeUtils {

    public static final String VERSION = "version";
    public static final String ACCEPT_HEADER_KEY_NAME = "Accept";

    /** Extract the version field value if present from a header or else null. */
    private static String extractVersion(String headerValue) {
        var contentType = ContentType.parse(headerValue);

        return contentType.getParameter(VERSION);
    }

    /** Extract mimetype field value if present from a header or else null. */
    private static String extractMimeType(String headerValue) {
        var contentType = ContentType.parse(headerValue);

        return contentType.getMimeType();
    }

    public static String extractVersionFromRequestInfo(RequestInfo requestInfo) {
        return Optional.of(requestInfo)
                .map(RequestInfo::getHeaders)
                .map(map -> map.get(ACCEPT_HEADER_KEY_NAME))
                .map(ContentTypeUtils::extractVersion)
                .orElse(null);
    }

    public static String extractMimeTypeFromRequestInfo(RequestInfo requestInfo) {
        return Optional.of(requestInfo)
                .map(RequestInfo::getHeaders)
                .map(map -> map.get(ACCEPT_HEADER_KEY_NAME))
                .map(ContentTypeUtils::extractMimeType)
                .orElse(null);
    }
}
