package no.unit.nva.search;

import no.unit.nva.auth.CognitoCredentials;
import nva.commons.core.JacocoGenerated;
import org.apache.http.HttpException;
import org.apache.http.HttpRequest;
import org.apache.http.HttpRequestInterceptor;
import org.apache.http.protocol.HttpContext;

import java.io.IOException;

import static com.amazonaws.auth.internal.SignerConstants.AUTHORIZATION;

public class CognitoInterceptor implements HttpRequestInterceptor {
    private final CognitoCredentials credentials;

    @JacocoGenerated
    public CognitoInterceptor(CognitoCredentials credentials) {
        this.credentials = credentials;
    }

    @Override
    @JacocoGenerated
    public void process(HttpRequest request, HttpContext context) throws HttpException, IOException {

        request.addHeader(AUTHORIZATION, "");
    }
}
