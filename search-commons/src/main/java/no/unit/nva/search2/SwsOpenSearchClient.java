package no.unit.nva.search2;

import static no.unit.nva.search.RestHighLevelClientWrapper.SEARCH_INFRASTRUCTURE_CREDENTIALS;
import static no.unit.nva.search.constants.ApplicationConstants.SEARCH_INFRASTRUCTURE_AUTH_URI;
import static nva.commons.core.attempt.Try.attempt;
import java.net.URI;
import no.unit.nva.auth.CognitoCredentials;
import no.unit.nva.auth.uriretriever.AuthorizedBackendUriRetriever;
import no.unit.nva.search.models.SearchResponseDto;
import no.unit.nva.search.models.UsernamePasswordWrapper;
import nva.commons.apigateway.GatewayResponse;
import nva.commons.core.JacocoGenerated;
import nva.commons.secrets.SecretsReader;

public class SwsOpenSearchClient {

    private CognitoCredentials cognito;

    /**
     * Creates a new OpensearchClient.
     *
     * @param  cognito cognito credentials
     */
    public SwsOpenSearchClient(CognitoCredentials cognito) {
        this.cognito = cognito;
    }


    @JacocoGenerated
    public static SwsOpenSearchClient defaultSwsClient() {
        return prepareWithSecretReader(new SecretsReader());
    }

    public static SwsOpenSearchClient prepareWithSecretReader(SecretsReader secretReader) {
        return new SwsOpenSearchClient(createCognitoCredentials(secretReader));
    }

    protected GatewayResponse<SearchResponseDto> doSearch(URI requestUri) {

        var response =
            new AuthorizedBackendUriRetriever(
                cognito.getCognitoOAuthServerUri().toString(),
                cognito.getCognitoAppClientId()
            ).fetchResponse(requestUri, "application/json")
                .orElseThrow();


        return attempt(() -> GatewayResponse.<SearchResponseDto>of(response.toString()))
                   .orElseThrow();
    }


    protected static CognitoCredentials createCognitoCredentials(SecretsReader secretsReader) {
        var credentials
            = secretsReader.fetchClassSecret(SEARCH_INFRASTRUCTURE_CREDENTIALS, UsernamePasswordWrapper.class);
        var uri = URI.create(SEARCH_INFRASTRUCTURE_AUTH_URI);

        return new CognitoCredentials(credentials::getUsername, credentials::getPassword, uri);
    }
}
