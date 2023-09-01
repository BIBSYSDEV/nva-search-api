package no.unit.nva.search2;

import com.fasterxml.jackson.core.JsonProcessingException;
import no.unit.nva.auth.CognitoCredentials;
import no.unit.nva.auth.uriretriever.AuthorizedBackendUriRetriever;
import no.unit.nva.auth.uriretriever.RawContentRetriever;
import no.unit.nva.search.models.SearchResponseDto;
import no.unit.nva.search2.common.OpenSearchResponseDto;
import no.unit.nva.search2.common.UsernamePasswordWrapper;
import nva.commons.apigateway.GatewayResponse;
import nva.commons.core.JacocoGenerated;
import nva.commons.secrets.SecretsReader;
import org.jetbrains.annotations.NotNull;

import java.net.URI;
import java.net.http.HttpResponse;
import java.util.function.Function;

import static no.unit.nva.search.RestHighLevelClientWrapper.SEARCH_INFRASTRUCTURE_CREDENTIALS;
import static no.unit.nva.search.constants.ApplicationConstants.SEARCH_INFRASTRUCTURE_AUTH_URI;

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


    protected SearchResponseDto doSearch(URI requestUri) {
        return
            contentRetriever.fetchResponse(requestUri, mediaType).stream()
                .map(HttpResponse::toString)
                .map(toGateWayResponse())
                .map(toInstance())
                .map(instance -> instance.toSearchResponseDto(requestUri))
                .findFirst()
                .orElseThrow();
    }

    @NotNull
    private Function<GatewayResponse<OpenSearchResponseDto>, OpenSearchResponseDto> toInstance() {
        return response -> {
            try {
                return response.getBodyAsInstance();
            } catch (JsonProcessingException e) {
                throw new RuntimeException(e);
            }
        };
    }

    @NotNull
    private Function<String, GatewayResponse<OpenSearchResponseDto>> toGateWayResponse() {
        return jsonString -> {
            try {
                return GatewayResponse.of(jsonString);
            } catch (JsonProcessingException e) {
                throw new RuntimeException(e);
            }
        };
    }

    protected static CognitoCredentials createCognitoCredentials(SecretsReader secretsReader) {
        var credentials
            = secretsReader.fetchClassSecret(SEARCH_INFRASTRUCTURE_CREDENTIALS, UsernamePasswordWrapper.class);
        var uri = URI.create(SEARCH_INFRASTRUCTURE_AUTH_URI);

        return new CognitoCredentials(credentials::getUsername, credentials::getPassword, uri);
    }
}
