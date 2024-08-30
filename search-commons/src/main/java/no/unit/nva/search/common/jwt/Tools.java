package no.unit.nva.search.common.jwt;

import static no.unit.nva.search.common.constant.Functions.readSearchInfrastructureAuthUri;
import static no.unit.nva.search.common.constant.Words.SEARCH_INFRASTRUCTURE_CREDENTIALS;

import java.net.URI;
import java.util.stream.Stream;
import no.unit.nva.auth.CognitoCredentials;
import no.unit.nva.search.common.records.UsernamePasswordWrapper;
import nva.commons.core.JacocoGenerated;
import nva.commons.secrets.SecretsReader;

public final class Tools {
  @JacocoGenerated
  public Tools() {}

  @JacocoGenerated
  public static CachedJwtProvider getCachedJwtProvider(SecretsReader reader) {
    return getUsernamePasswordStream(reader)
        .map(Tools::getCognitoCredentials)
        .map(CognitoAuthenticator::prepareWithCognitoCredentials)
        .map(CachedJwtProvider::prepareWithAuthenticator)
        .findFirst()
        .orElseThrow();
  }

  @JacocoGenerated
  public static Stream<UsernamePasswordWrapper> getUsernamePasswordStream(
      SecretsReader secretsReader) {
    return Stream.of(
        secretsReader.fetchClassSecret(
            SEARCH_INFRASTRUCTURE_CREDENTIALS, UsernamePasswordWrapper.class));
  }

  @JacocoGenerated
  public static CognitoCredentials getCognitoCredentials(UsernamePasswordWrapper wrapper) {
    var uri = URI.create(readSearchInfrastructureAuthUri());
    return new CognitoCredentials(wrapper::getUsername, wrapper::getPassword, uri);
  }
}
