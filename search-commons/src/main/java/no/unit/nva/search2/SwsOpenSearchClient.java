package no.unit.nva.search2;

import static no.unit.nva.search.RestHighLevelClientWrapper.SEARCH_INFRASTRUCTURE_CREDENTIALS;
import static no.unit.nva.search.constants.ApplicationConstants.SEARCH_INFRASTRUCTURE_AUTH_URI;
import com.fasterxml.jackson.core.JsonProcessingException;
import java.net.URI;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import no.unit.nva.auth.CognitoCredentials;
import no.unit.nva.auth.uriretriever.AuthorizedBackendUriRetriever;
import no.unit.nva.search.models.SearchResponseDto;
import no.unit.nva.search.models.UsernamePasswordWrapper;
import no.unit.nva.search2.common.OpenSearchResponseDto;
import nva.commons.apigateway.GatewayResponse;
import nva.commons.core.JacocoGenerated;
import nva.commons.secrets.SecretsReader;
import org.opensearch.common.ParseField;
import org.opensearch.common.xcontent.ContextParser;
import org.opensearch.common.xcontent.NamedXContentRegistry;
import org.opensearch.common.xcontent.NamedXContentRegistry.Entry;
import org.opensearch.search.aggregations.Aggregation;
import org.opensearch.search.aggregations.bucket.terms.ParsedStringTerms;
import org.opensearch.search.aggregations.bucket.terms.StringTerms;
import org.opensearch.search.aggregations.metrics.ParsedTopHits;
import org.opensearch.search.aggregations.metrics.TopHitsAggregationBuilder;

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

    protected SearchResponseDto doSearch(URI requestUri) {

        var response =
            new AuthorizedBackendUriRetriever(
                cognito.getCognitoOAuthServerUri().toString(),
                cognito.getCognitoAppClientId()
            ).fetchResponse(requestUri, "application/json")
                .orElseThrow();
        try {
            var gatewayResponse =
                GatewayResponse.<OpenSearchResponseDto>of(response.toString());

            return gatewayResponse.getBodyAsInstance().toSearchResponseDto(requestUri);

        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }


    private static List<Entry> getDefaultNamedXContents() {
        Map<String, ContextParser<Object, ? extends Aggregation>> map = new HashMap<>();
        map.put(TopHitsAggregationBuilder.NAME, (p, c) -> ParsedTopHits.fromXContent(p, (String) c));
        map.put(StringTerms.NAME, (p, c) -> ParsedStringTerms.fromXContent(p, (String) c));
        return
            map.entrySet().stream()
                .map(entry -> new NamedXContentRegistry.Entry(
                    Aggregation.class, new ParseField(entry.getKey()), entry.getValue()))
                .collect(Collectors.toList());
    }

    protected static CognitoCredentials createCognitoCredentials(SecretsReader secretsReader) {
        var credentials
            = secretsReader.fetchClassSecret(SEARCH_INFRASTRUCTURE_CREDENTIALS, UsernamePasswordWrapper.class);
        var uri = URI.create(SEARCH_INFRASTRUCTURE_AUTH_URI);

        return new CognitoCredentials(credentials::getUsername, credentials::getPassword, uri);
    }
}
