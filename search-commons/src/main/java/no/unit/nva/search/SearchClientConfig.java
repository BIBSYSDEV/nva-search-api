package no.unit.nva.search;

import no.unit.nva.auth.CognitoCredentials;
import no.unit.nva.search.models.UsernamePasswordWrapper;
import nva.commons.core.JacocoGenerated;
import nva.commons.secrets.SecretsReader;
import org.apache.http.HttpHost;
import org.apache.http.HttpRequestInterceptor;
import org.opensearch.client.RestClient;
import org.opensearch.client.RestClientBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;

import static no.unit.nva.search.constants.ApplicationConstants.SEARCH_INFRASTRUCTURE_API_URI;
import static no.unit.nva.search.constants.ApplicationConstants.SEARCH_INFRASTRUCTURE_AUTH_URI;

public final class SearchClientConfig {

    public static final String INITIAL_LOG_MESSAGE = "Connecting to Elasticsearch at {}";
    private static final Logger logger = LoggerFactory.getLogger(SearchClientConfig.class);
    public static final String SEARCH_INFRASTRUCTURE_CREDENTIALS = "SearchInfrastructureCredentials";

    private SearchClientConfig() {

    }


    @JacocoGenerated
    public static SearchClient defaultSearchClient() {
        return new SearchClient(defaultRestHighLevelClientWrapper());
    }

    public static SearchClient prepareWithSecretReader(SecretsReader secretReader) {
        return new SearchClient(prepareRestHighLevelClientWithSecretReader(secretReader));
    }

    @JacocoGenerated
    public static RestHighLevelClientWrapper defaultRestHighLevelClientWrapper() {
        return createElasticsearchClientWithInterceptor(new SecretsReader(), SEARCH_INFRASTRUCTURE_API_URI);
    }

    public static RestHighLevelClientWrapper prepareRestHighLevelClientWithSecretReader(SecretsReader secretReader) {
        return createElasticsearchClientWithInterceptor(secretReader, SEARCH_INFRASTRUCTURE_API_URI);
    }

    private static RestHighLevelClientWrapper createElasticsearchClientWithInterceptor(SecretsReader secretsReader,
                                                                                       String address) {
        logger.info(INITIAL_LOG_MESSAGE, address);

        var cognitoCredentials = createCognitoCredentials(secretsReader);
        var authenticator = CognitoAuthenticator.prepareWithCognitoCredentials(cognitoCredentials);

        HttpRequestInterceptor interceptor = new CognitoInterceptor(authenticator);

        RestClientBuilder clientBuilder = RestClient
                .builder(HttpHost.create(address))
                .setHttpClientConfigCallback(config -> config.addInterceptorLast(interceptor));
        return new RestHighLevelClientWrapper(clientBuilder);
    }

    private static CognitoCredentials createCognitoCredentials(SecretsReader secretsReader) {
        var credentials
                = secretsReader.fetchClassSecret(SEARCH_INFRASTRUCTURE_CREDENTIALS, UsernamePasswordWrapper.class);
        var uri = URI.create(SEARCH_INFRASTRUCTURE_AUTH_URI);

        return new CognitoCredentials(credentials::getUsername, credentials::getPassword, uri);
    }

}
