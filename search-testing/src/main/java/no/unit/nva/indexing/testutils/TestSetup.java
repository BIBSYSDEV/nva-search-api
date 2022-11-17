package no.unit.nva.indexing.testutils;

import static no.unit.nva.indexing.testutils.TestConstants.TEST_TOKEN;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import com.auth0.jwt.interfaces.DecodedJWT;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;
import no.unit.nva.search.CachedJwtProvider;
import no.unit.nva.search.CognitoAuthenticator;

public class TestSetup {

    public static CachedJwtProvider setupMockedCachedJwtProvider() {
        var jwt = mock(DecodedJWT.class);
        var cogintoAuthenticatorMock = mock(CognitoAuthenticator.class);

        when(jwt.getToken()).thenReturn(TEST_TOKEN);
        when(jwt.getExpiresAt()).thenReturn(Date.from(Instant.now().plus(Duration.ofMinutes(5))));
        when(cogintoAuthenticatorMock.fetchBearerToken()).thenReturn(jwt);

        return CachedJwtProvider.prepareWithAuthenticator(cogintoAuthenticatorMock);
    }
}
