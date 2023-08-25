package no.unit.nva.search2;

import static no.unit.nva.search.RestHighLevelClientWrapper.SEARCH_INFRASTRUCTURE_CREDENTIALS;
import static no.unit.nva.search.constants.ApplicationConstants.SEARCH_INFRASTRUCTURE_AUTH_URI;
import static org.opensearch.common.xcontent.DeprecationHandler.IGNORE_DEPRECATIONS;
import java.io.IOException;
import java.net.URI;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import no.unit.nva.auth.CognitoCredentials;
import no.unit.nva.auth.uriretriever.AuthorizedBackendUriRetriever;
import no.unit.nva.search.models.UsernamePasswordWrapper;
import nva.commons.apigateway.exceptions.BadGatewayException;
import nva.commons.core.JacocoGenerated;
import nva.commons.secrets.SecretsReader;
import org.opensearch.action.search.SearchResponse;
import org.opensearch.common.ParseField;
import org.opensearch.common.xcontent.ContextParser;
import org.opensearch.common.xcontent.NamedXContentRegistry;
import org.opensearch.common.xcontent.NamedXContentRegistry.Entry;
import org.opensearch.common.xcontent.XContentParser;
import org.opensearch.common.xcontent.json.JsonXContent;
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

    protected SearchResponse doSearch(URI requestUri) throws BadGatewayException {

        var response =
            new AuthorizedBackendUriRetriever(
                cognito.getCognitoOAuthServerUri().toString(),
                cognito.getCognitoAppClientId()
            ).fetchResponse(requestUri, "application/json")
                .orElseThrow();

        var registry = new NamedXContentRegistry(getDefaultNamedXContents());
        try (var parser = JsonXContent.jsonXContent.createParser(
            registry, IGNORE_DEPRECATIONS, response.body())) {
            return SearchResponse.fromXContent(parser);
        } catch (IOException e) {
            throw new BadGatewayException(e.getMessage());
        }
        //        var typeReference = new TypeReference<SearchResponseDto>() { };
//        try {
//            return objectMapper.readValue(response.body(),typeReference);
//        } catch (JsonProcessingException e) {
//            throw new RuntimeException(e);
//        }

        //        return attempt(() -> GatewayResponse.<SearchResponseDto>of(response.toString()))
//                   .orElseThrow();
    }


    private static List<Entry> getDefaultNamedXContents() {
        Map<String, ContextParser<Object, ? extends Aggregation>> map = new HashMap<>();
        map.put(TopHitsAggregationBuilder.NAME, (p, c) -> ParsedTopHits.fromXContent(p, (String) c));
        map.put(StringTerms.NAME, (p, c) -> ParsedStringTerms.fromXContent(p, (String) c));
        List<NamedXContentRegistry.Entry> entries = map.entrySet().stream()
                                                        .map(entry -> new NamedXContentRegistry.Entry(
                                                            Aggregation.class, new ParseField(entry.getKey()), entry.getValue()))
                                                        .collect(Collectors.toList());
        return entries;
    }

    protected static CognitoCredentials createCognitoCredentials(SecretsReader secretsReader) {
        var credentials
            = secretsReader.fetchClassSecret(SEARCH_INFRASTRUCTURE_CREDENTIALS, UsernamePasswordWrapper.class);
        var uri = URI.create(SEARCH_INFRASTRUCTURE_AUTH_URI);

        return new CognitoCredentials(credentials::getUsername, credentials::getPassword, uri);
    }
}
