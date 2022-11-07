package no.unit.nva.search;

import com.fasterxml.jackson.jr.ob.JSON;
import no.unit.nva.auth.CognitoCredentials;
import nva.commons.core.JacocoGenerated;
import nva.commons.core.paths.UriWrapper;

import java.net.HttpURLConnection;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Objects;

import static com.amazonaws.auth.internal.SignerConstants.AUTHORIZATION;
import static no.unit.nva.auth.AuthorizedBackendClient.APPLICATION_X_WWW_FORM_URLENCODED;
import static nva.commons.core.attempt.Try.attempt;
import static org.apache.http.protocol.HTTP.CONTENT_TYPE;

public class CognitoAuthenticator {
    private final CognitoCredentials credentials;
    public static final String AUTHORIZATION_ERROR_MESSAGE = "Could not authorizer client";
    public static final String GRANT_TYPE_CLIENT_CREDENTIALS = "grant_type=client_credentials";
    public static final String JWT_TOKEN_FIELD = "access_token";

    private final HttpClient httpClient;

    public CognitoAuthenticator(HttpClient httpClient, CognitoCredentials credentials) {
        this.httpClient = httpClient;
        this.credentials = credentials;
    }

    @JacocoGenerated
    public static CognitoAuthenticator prepareWithCognitoCredentials(CognitoCredentials cognitoCredentials) {
        return prepareWithCognitoCredentials(HttpClient.newHttpClient(), cognitoCredentials);
    }

    public static CognitoAuthenticator prepareWithCognitoCredentials(HttpClient httpClient,
                                                                     CognitoCredentials cognitoApiClientCredentials) {
        return new CognitoAuthenticator(httpClient, cognitoApiClientCredentials);
    }

    public String getBearerToken() {
        var tokenUri = standardOauth2TokenEndpoint(credentials.getCognitoOAuthServerUri());
        var request = formatRequestForJwtToken(tokenUri);
        return sendRequestAndExtractToken(request);
    }

    private static URI standardOauth2TokenEndpoint(URI cognitoHost) {
        return UriWrapper.fromUri(cognitoHost).addChild("oauth2").addChild("token").getUri();
    }

    private static HttpRequest.BodyPublisher clientCredentialsAuthType() {
        return HttpRequest.BodyPublishers.ofString(GRANT_TYPE_CLIENT_CREDENTIALS);
    }

    private String formatAuthenticationHeaderValue() {
        return String.format("%s:%s",
                credentials.getCognitoAppClientId(),
                credentials.getCognitoAppClientSecret());
    }

    private String formatBasicAuthenticationHeader() {
        return attempt(this::formatAuthenticationHeaderValue)
                .map(str -> Base64.getEncoder().encodeToString(str.getBytes(StandardCharsets.UTF_8)))
                .map(credentials -> "Basic " + credentials)
                .orElseThrow();
    }

    private String sendRequestAndExtractToken(HttpRequest request) {
        var response = attempt(
                        () -> this.httpClient.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8))
                    )
                .map(this::responseIsSuccessful)
                .orElseThrow();

        return attempt(() -> response)
                .map(HttpResponse::body)
                .map(JSON.std::mapFrom)
                .map(json -> json.get(JWT_TOKEN_FIELD))
                .toOptional()
                .map(Objects::toString)
                .map(this::createBearerToken)
                .orElseThrow();
    }

    private HttpResponse<String> responseIsSuccessful(HttpResponse<String> response) {
        if (HttpURLConnection.HTTP_OK != response.statusCode()) {
            throw new RuntimeException(AUTHORIZATION_ERROR_MESSAGE);
        }
        return response;
    }

    private HttpRequest formatRequestForJwtToken(URI tokenUri) {
        return HttpRequest.newBuilder(tokenUri)
                .setHeader(AUTHORIZATION, formatBasicAuthenticationHeader())
                .setHeader(CONTENT_TYPE, APPLICATION_X_WWW_FORM_URLENCODED)
                .POST(clientCredentialsAuthType())
                .build();
    }

    private String createBearerToken(String accessToken) {
        return "Bearer " + accessToken;
    }
}
