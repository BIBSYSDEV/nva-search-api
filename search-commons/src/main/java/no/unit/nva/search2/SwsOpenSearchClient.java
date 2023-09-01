package no.unit.nva.search2;

import no.unit.nva.auth.CognitoCredentials;
import no.unit.nva.auth.uriretriever.AuthorizedBackendUriRetriever;
import no.unit.nva.auth.uriretriever.RawContentRetriever;
import no.unit.nva.search.models.UsernamePasswordWrapper;
import no.unit.nva.search2.common.GatewayResponse;
import no.unit.nva.search2.common.SwsOpenSearchResponse;
import nva.commons.core.JacocoGenerated;
import nva.commons.secrets.SecretsReader;
import org.jetbrains.annotations.NotNull;

import java.net.URI;
import java.net.http.HttpResponse;
import java.util.Map;
import java.util.function.Function;

import static no.unit.nva.search.RestHighLevelClientWrapper.SEARCH_INFRASTRUCTURE_CREDENTIALS;
import static no.unit.nva.search.constants.ApplicationConstants.SEARCH_INFRASTRUCTURE_AUTH_URI;
import static no.unit.nva.search.constants.ApplicationConstants.objectMapperNoEmpty;
import static nva.commons.core.attempt.Try.attempt;

public class SwsOpenSearchClient {

    private final RawContentRetriever contentRetriever;
    private final  String mediaType;

    public SwsOpenSearchClient(RawContentRetriever contentRetriever, String mediaType) {
        this.contentRetriever = contentRetriever;
        this.mediaType = mediaType;
    }


    @JacocoGenerated
    public static SwsOpenSearchClient defaultSwsClient() {
        return prepareWithSecretReader(new SecretsReader());
    }

    public static SwsOpenSearchClient prepareWithSecretReader(SecretsReader secretReader) {
        var cognito = createCognitoCredentials(secretReader);
        var retriver = new AuthorizedBackendUriRetriever(
            cognito.getCognitoOAuthServerUri().toString(),
            cognito.getCognitoAppClientId());

        return new SwsOpenSearchClient(retriver, "application/json" );
    }


    protected GatewayResponse<SwsOpenSearchResponse> doSearch(URI requestUri) {
        return
            contentRetriever.fetchResponse(requestUri, mediaType)
                .map(toOpenSearchResponse())
                .orElseThrow();
    }

    @NotNull
    private Function<HttpResponse<String>, GatewayResponse<SwsOpenSearchResponse>> toOpenSearchResponse() {
        return response -> {
            var openSearchResponseDto =
                attempt(()->objectMapperNoEmpty.readValue(response.body(), SwsOpenSearchResponse.class)).orElseThrow();
            var statusCode = response.statusCode();
            var headers = response.headers().map();
            var requestUri = response.request().uri();

            return new GatewayResponse<>(openSearchResponseDto, statusCode, Map.of(), requestUri);
        };
    }

    protected static CognitoCredentials createCognitoCredentials(SecretsReader secretsReader) {
        var credentials
            = secretsReader.fetchClassSecret(SEARCH_INFRASTRUCTURE_CREDENTIALS, UsernamePasswordWrapper.class);
        var uri = URI.create(SEARCH_INFRASTRUCTURE_AUTH_URI);

        return new CognitoCredentials(credentials::getUsername, credentials::getPassword, uri);
    }
}
