package no.unit.nva.search;

import nva.commons.core.JacocoGenerated;
import org.apache.http.HttpRequest;
import org.apache.http.HttpRequestInterceptor;
import org.apache.http.protocol.HttpContext;

import static com.amazonaws.auth.internal.SignerConstants.AUTHORIZATION;

public class CognitoInterceptor implements HttpRequestInterceptor {
    private final CognitoAuthenticator authenticator;

    @JacocoGenerated
    public CognitoInterceptor(CognitoAuthenticator authenticator) {
        this.authenticator = authenticator;
    }

    @Override
    @JacocoGenerated
    public void process(HttpRequest request, HttpContext context) {
        request.addHeader(AUTHORIZATION, authenticator.getBearerToken());
    }
}
