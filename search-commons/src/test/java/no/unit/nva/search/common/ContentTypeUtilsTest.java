package no.unit.nva.search.common;

import static no.unit.nva.search.common.ContentTypeUtils.ACCEPT_HEADER_KEY_NAME;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;

import nva.commons.apigateway.RequestInfo;

import org.junit.jupiter.api.Test;

import java.util.Map;

class ContentTypeUtilsTest {
    public static final String ACCEPT_HEADER_VALUE = "application/json; version=2023-05-10";
    public static final String ACCEPT_HEADER_VALUE_WITH_QUOTES =
            "application/json; version=\"2023-05-10\"";
    public static final String ACCEPT_HEADER_VALUE_WITHOUT_VERSION = "application/json";
    public static final String VERSION_VALUE = "2023-05-10";
    public static final String MIME_TYPE = "application/json";

    @Test
    void asssertThatMimeTypeAndVersionIsExtractedWhenProvided() {
        RequestInfo requestInfo = new RequestInfo();
        requestInfo.setHeaders(Map.of(ACCEPT_HEADER_KEY_NAME, ACCEPT_HEADER_VALUE));

        var version = ContentTypeUtils.extractVersionFromRequestInfo(requestInfo);
        var mimeType = ContentTypeUtils.extractMimeTypeFromRequestInfo(requestInfo);

        assertThat(mimeType, equalTo(MIME_TYPE));
        assertThat(version, equalTo(VERSION_VALUE));
    }

    @Test
    void asssertThatMimeTypeAndVersionIsExtractedWhenProvidedAndVersionHasQuotes() {
        RequestInfo requestInfo = new RequestInfo();
        requestInfo.setHeaders(Map.of(ACCEPT_HEADER_KEY_NAME, ACCEPT_HEADER_VALUE_WITH_QUOTES));

        var version = ContentTypeUtils.extractVersionFromRequestInfo(requestInfo);
        var mimeType = ContentTypeUtils.extractMimeTypeFromRequestInfo(requestInfo);

        assertThat(mimeType, equalTo(MIME_TYPE));
        assertThat(version, equalTo(VERSION_VALUE));
    }

    @Test
    void asssertThatMimeTypeAndVersionAreNullWhenNotProvided() {
        RequestInfo requestInfo = new RequestInfo();
        requestInfo.setHeaders(Map.of());

        var version = ContentTypeUtils.extractVersionFromRequestInfo(requestInfo);
        var mimeType = ContentTypeUtils.extractMimeTypeFromRequestInfo(requestInfo);

        assertThat(mimeType, equalTo(null));
        assertThat(version, equalTo(null));
    }
}
