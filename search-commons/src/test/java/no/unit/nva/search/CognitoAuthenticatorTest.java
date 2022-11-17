package no.unit.nva.search;

import no.unit.nva.auth.CognitoCredentials;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpResponse;
import java.util.NoSuchElementException;

import static java.net.HttpURLConnection.HTTP_FORBIDDEN;
import static java.net.HttpURLConnection.HTTP_OK;
import static no.unit.nva.indexing.testutils.TestConstants.TEST_SCOPE;
import static no.unit.nva.indexing.testutils.TestConstants.TEST_TOKEN;
import static no.unit.nva.search.CognitoAuthenticator.AUTHORIZATION_ERROR_MESSAGE;
import static no.unit.nva.testutils.RandomDataGenerator.randomString;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class CognitoAuthenticatorTest {

    private CognitoAuthenticator cognitoAuthenticator;

    final HttpClient httpClient = mock(HttpClient.class);

    HttpResponse<String> okResponse = mock(HttpResponse.class);
    HttpResponse<String> invalidResponse = mock(HttpResponse.class);
    HttpResponse<String> errorResponse = mock(HttpResponse.class);

    @BeforeEach
    public void setup() {
        var authServer = "http://localhost";
        var clientId = randomString();
        var clientSecret = randomString();
        var credentials = new CognitoCredentials(() -> clientId, () -> clientSecret, URI.create(authServer));
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
        when(httpClient.<String>send(any(),any())).thenReturn(okResponse);

        var jwt = cognitoAuthenticator.getBearerToken();
        assertThat(jwt.getToken(), is(TEST_TOKEN));
    }

    @Test
    void shouldReturnDecodedJwtWithClaims() throws IOException, InterruptedException {
        when(httpClient.<String>send(any(),any())).thenReturn(okResponse);

        var jwt = cognitoAuthenticator.getBearerToken();
        assertThat(jwt.getClaim("scope").asString(), is(TEST_SCOPE));
    }

    @Test
    void shouldThrowWhenResponseIsNotStructuedLikeAToken() throws IOException, InterruptedException {
        when(httpClient.<String>send(any(),any())).thenReturn(invalidResponse);
        assertThrows(NoSuchElementException.class, () -> cognitoAuthenticator.getBearerToken());
    }

    @Test
    void shouldThrowWhenResponseIsNot200Ok() throws IOException, InterruptedException {
        when(httpClient.<String>send(any(),any())).thenReturn(errorResponse);
        var exception = assertThrows(RuntimeException.class, () -> cognitoAuthenticator.getBearerToken());
        assertEquals(AUTHORIZATION_ERROR_MESSAGE, exception.getMessage());
    }
}