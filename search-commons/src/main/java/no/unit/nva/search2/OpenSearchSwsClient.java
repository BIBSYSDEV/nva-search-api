package no.unit.nva.search2;

import no.unit.nva.auth.AuthorizedBackendClient;
import no.unit.nva.auth.CognitoCredentials;
import no.unit.nva.search.CachedJwtProvider;
import no.unit.nva.search.CognitoAuthenticator;
import no.unit.nva.search.models.UsernamePasswordWrapper;
import no.unit.nva.search2.model.OpenSearchSwsResponse;
import nva.commons.apigateway.exceptions.BadGatewayException;
import nva.commons.core.JacocoGenerated;
import nva.commons.secrets.SecretsReader;
import org.apache.http.HttpStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;

import static no.unit.nva.auth.uriretriever.AuthorizedBackendUriRetriever.ACCEPT;
import static no.unit.nva.search.RestHighLevelClientWrapper.SEARCH_INFRASTRUCTURE_CREDENTIALS;
import static no.unit.nva.search2.constant.ApplicationConstants.objectMapperWithEmpty;
import static no.unit.nva.search2.constant.ApplicationConstants.readSearchInfrastructureAuthUri;
import static nva.commons.core.attempt.Try.attempt;
import static org.apache.http.entity.ContentType.APPLICATION_JSON;

public class OpenSearchSwsClient {

    private static final Logger logger = LoggerFactory.getLogger(OpenSearchSwsClient.class);
    private static final String REQUESTING_SEARCH_FROM = "OpenSearchSwsClient url -> {}";
    private final AuthorizedBackendClient contentRetriever;
    private final String mediaType;

    public OpenSearchSwsClient(AuthorizedBackendClient contentRetriever, String mediaType) {
        this.contentRetriever = contentRetriever;
        this.mediaType = mediaType;
    }

    @JacocoGenerated
    public static OpenSearchSwsClient defaultSwsClient() {
        var uri = URI.create(readSearchInfrastructureAuthUri());
        var credentials
            = new SecretsReader()
            .fetchClassSecret(SEARCH_INFRASTRUCTURE_CREDENTIALS, UsernamePasswordWrapper.class);

        var cognitoCredentials = new CognitoCredentials(credentials::getUsername, credentials::getPassword, uri);
        var cognitoAuthenticator = CognitoAuthenticator.prepareWithCognitoCredentials(cognitoCredentials);
        var cachedJwtProvider = CachedJwtProvider.prepareWithAuthenticator(cognitoAuthenticator);
        var retriever = AuthorizedBackendClient.prepareWithBearerToken(cachedJwtProvider.getValue().getToken());

        return new OpenSearchSwsClient(retriever, APPLICATION_JSON.toString());
    }

    protected OpenSearchSwsResponse doSearch(URI requestUri)
        throws BadGatewayException {
        logger.info(REQUESTING_SEARCH_FROM, requestUri);
        var request = HttpRequest.newBuilder(requestUri).headers(ACCEPT, mediaType).GET();
        var bodyHandler = HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8);
        var response = attempt(() -> contentRetriever.send(request, bodyHandler)).orElseThrow();
        if (response.statusCode() != HttpStatus.SC_OK) {
            throw new BadGatewayException(response.body());
        }
        return attempt(() -> objectMapperWithEmpty.readValue(response.body(), OpenSearchSwsResponse.class))
            .orElseThrow();
    }
}
