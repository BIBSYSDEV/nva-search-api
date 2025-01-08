package no.unit.nva.indexing.testutils;

import static no.unit.nva.indexing.testutils.Constants.TEST_TOKEN;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.auth0.jwt.interfaces.DecodedJWT;

import no.unit.nva.search.common.jwt.CachedJwtProvider;
import no.unit.nva.search.common.jwt.CognitoAuthenticator;

import nva.commons.core.JacocoGenerated;

import java.time.Duration;
import java.time.Instant;
import java.util.Date;

public final class MockedJwtProvider {

    @JacocoGenerated
    private MockedJwtProvider() {}

    public static CachedJwtProvider setupMockedCachedJwtProvider() {
        var jwt = mock(DecodedJWT.class);
        var cogintoAuthenticatorMock = mock(CognitoAuthenticator.class);

        when(jwt.getToken()).thenReturn(TEST_TOKEN);
        when(jwt.getExpiresAt()).thenReturn(Date.from(Instant.now().plus(Duration.ofMinutes(5))));
        when(cogintoAuthenticatorMock.fetchBearerToken()).thenReturn(jwt);

        return CachedJwtProvider.prepareWithAuthenticator(cogintoAuthenticatorMock);
    }
}
