package no.umit.nva.search.model;


import static no.unit.nva.auth.AuthorizedBackendClient.APPLICATION_X_WWW_FORM_URLENCODED;
import static no.unit.nva.search.model.constant.Words.AUTHORIZATION;
import static no.unit.nva.search.model.constant.Words.CONTENT_TYPE;
import static no.unit.nva.testutils.RandomDataGenerator.randomString;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import static java.net.HttpURLConnection.HTTP_FORBIDDEN;
import static java.net.HttpURLConnection.HTTP_OK;

import no.unit.nva.auth.CognitoCredentials;
import no.unit.nva.search.model.jwt.CognitoAuthenticator;

import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpRequest.BodyPublishers;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.NoSuchElementException;

@SuppressWarnings({"unchecked"})
class CognitoAuthenticatorTest {

    final HttpClient httpClient = mock(HttpClient.class);
    final HttpResponse<String> okResponse = mock(HttpResponse.class);
    final HttpResponse<String> invalidResponse = mock(HttpResponse.class);
    final HttpResponse<String> errorResponse = mock(HttpResponse.class);
    private CognitoAuthenticator cognitoAuthenticator;
    private CognitoCredentials credentials;

    @BeforeEach
    public void setup() {
        var authServer = "http://localhost";
        var clientId = randomString();
        var clientSecret = randomString();
        credentials =
                new CognitoCredentials(() -> clientId, () -> clientSecret, URI.create(authServer));
        cognitoAuthenticator = new CognitoAuthenticator(httpClient, credentials);

        when(okResponse.statusCode()).thenReturn(HTTP_OK);
        when(okResponse.body()).thenReturn("{\"access_token\": \"" + TEST_TOKEN + "\"}");

        when(invalidResponse.statusCode()).thenReturn(HTTP_OK);
        when(invalidResponse.body()).thenReturn("{}");

        when(errorResponse.statusCode()).thenReturn(HTTP_FORBIDDEN);
        when(errorResponse.body()).thenReturn("{}");
    }

    @Test
    void shouldReturnJwtTokenFromHttpRequestToCognito() throws IOException, InterruptedException {
        when(httpClient.<String>send(any(), any())).thenReturn(okResponse);

        var jwt = cognitoAuthenticator.fetchBearerToken();
        MatcherAssert.assertThat(jwt.getToken(), Matchers.is(TEST_TOKEN));
    }

    @Test
    void shouldReturnDecodedJwtWithClaims() throws IOException, InterruptedException {
        when(httpClient.<String>send(any(), any())).thenReturn(okResponse);

        var jwt = cognitoAuthenticator.fetchBearerToken();
        MatcherAssert.assertThat(jwt.getClaim("scope").asString(), Matchers.is(TEST_SCOPE));
    }

    @Test
    void shouldReturnDecodedJwtWhenSendingBasicAuthentication()
            throws IOException, InterruptedException {
        var uri = URI.create(credentials.getCognitoOAuthServerUri().toString() + "/oauth2/token");
        var usernamePassword =
                credentials.getCognitoAppClientId() + ":" + credentials.getCognitoAppClientSecret();
        var encodedAuth =
                Base64.getEncoder()
                        .encodeToString(usernamePassword.getBytes(StandardCharsets.UTF_8));

        var expectedRequest =
                HttpRequest.newBuilder()
                        .uri(uri)
                        .setHeader(AUTHORIZATION, "Basic " + encodedAuth)
                        .setHeader(CONTENT_TYPE, APPLICATION_X_WWW_FORM_URLENCODED)
                        .POST(BodyPublishers.noBody())
                        .build();

        when(httpClient.<String>send(
                        argThat(new HttpRequestMetadataMatcher(expectedRequest)), any()))
                .thenReturn(okResponse);

        var jwt = cognitoAuthenticator.fetchBearerToken();
        assertNotNull(jwt);
    }

    @Test
    void shouldThrowWhenResponseIsNotStructuedLikeAToken()
            throws IOException, InterruptedException {
        when(httpClient.<String>send(any(), any())).thenReturn(invalidResponse);
        assertThrows(NoSuchElementException.class, () -> cognitoAuthenticator.fetchBearerToken());
    }

    @Test
    void shouldThrowWhenResponseIsNot200Ok() throws IOException, InterruptedException {
        when(httpClient.<String>send(any(), any())).thenReturn(errorResponse);
        var exception =
                assertThrows(RuntimeException.class, () -> cognitoAuthenticator.fetchBearerToken());
        Assertions.assertEquals(CognitoAuthenticator.AUTHORIZATION_ERROR_MESSAGE, exception.getMessage());
    }
}
