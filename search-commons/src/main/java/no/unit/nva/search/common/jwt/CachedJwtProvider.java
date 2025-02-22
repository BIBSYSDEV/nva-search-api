package no.unit.nva.search.common.jwt;

import com.auth0.jwt.interfaces.DecodedJWT;
import java.time.Clock;
import java.util.Date;

/**
 * Class for providing a cached JWT.
 *
 * @author Sondre Vestad
 */
public class CachedJwtProvider extends CachedValueProvider<DecodedJWT> {

  private final CognitoAuthenticator cognitoAuthenticator;
  private final Clock clock;

  public CachedJwtProvider(CognitoAuthenticator cognitoAuthenticator, Clock clock) {
    super();
    this.cognitoAuthenticator = cognitoAuthenticator;
    this.clock = clock;
  }

  public static CachedJwtProvider prepareWithAuthenticator(
      CognitoAuthenticator cognitoAuthenticator) {
    return new CachedJwtProvider(cognitoAuthenticator, Clock.systemDefaultZone());
  }

  @Override
  protected boolean isExpired() {
    var in5sec = clock.instant().plusMillis(5000);

    var expiresAtDate = cachedValue.getExpiresAt();
    var dateIn5Secs = Date.from(in5sec);

    return expiresAtDate.before(dateIn5Secs);
  }

  @Override
  protected DecodedJWT getNewValue() {
    return cognitoAuthenticator.fetchBearerToken();
  }
}
