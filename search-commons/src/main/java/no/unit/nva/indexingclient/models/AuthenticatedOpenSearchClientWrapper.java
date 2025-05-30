package no.unit.nva.indexingclient.models;

import static no.unit.nva.constants.Defaults.ENVIRONMENT;
import static no.unit.nva.constants.Words.AUTHORIZATION;

import java.net.URI;
import no.unit.nva.auth.CognitoCredentials;
import no.unit.nva.search.common.jwt.CachedJwtProvider;
import no.unit.nva.search.common.records.UsernamePasswordWrapper;
import nva.commons.secrets.SecretsReader;
import org.opensearch.client.RequestOptions;

public class AuthenticatedOpenSearchClientWrapper {

  static final String SEARCH_INFRASTRUCTURE_CREDENTIALS = "SearchInfrastructureCredentials";
  static final String SEARCH_INFRASTRUCTURE_AUTH_URI =
      ENVIRONMENT.readEnv("SEARCH_INFRASTRUCTURE_AUTH_URI");

  protected final RestHighLevelClientWrapper openSearchClient;
  protected final CachedJwtProvider cachedJwtProvider;

  /**
   * Creates a new OpensearchClient.
   *
   * @param openSearchClient client to use for access to external search infrastructure
   * @param cachedJwtProvider provider for JWT tokens
   */
  public AuthenticatedOpenSearchClientWrapper(
      RestHighLevelClientWrapper openSearchClient, CachedJwtProvider cachedJwtProvider) {
    this.openSearchClient = openSearchClient;
    this.cachedJwtProvider = cachedJwtProvider;
  }

  /**
   * Creates a new OpensearchClient.
   *
   * @param secretsReader reader for secrets (used to fetch the username and password for the search
   *     infrastructure)
   * @return CognitoCredentials for the search infrastructure
   */
  protected static CognitoCredentials createCognitoCredentials(SecretsReader secretsReader) {
    var credentials =
        secretsReader.fetchClassSecret(
            SEARCH_INFRASTRUCTURE_CREDENTIALS, UsernamePasswordWrapper.class);
    var uri = URI.create(SEARCH_INFRASTRUCTURE_AUTH_URI);

    return new CognitoCredentials(credentials::getUsername, credentials::getPassword, uri);
  }

  protected RequestOptions getRequestOptions() {
    var token = "Bearer " + cachedJwtProvider.getValue().getToken();
    return RequestOptions.DEFAULT.toBuilder().addHeader(AUTHORIZATION, token).build();
  }
}
