package no.unit.nva.search;

import com.amazonaws.auth.AWS4Signer;
import com.amazonaws.auth.AWSCredentialsProvider;
import com.amazonaws.auth.DefaultAWSCredentialsProviderChain;
import com.amazonaws.http.AWSRequestSigningApacheInterceptor;
import no.unit.nva.auth.CognitoCredentials;
import no.unit.nva.search.models.Credentials;
import nva.commons.secrets.SecretsReader;
import org.apache.http.HttpHost;
import org.apache.http.HttpRequestInterceptor;
import org.opensearch.client.RestClient;
import org.opensearch.client.RestClientBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;

import static no.unit.nva.search.constants.ApplicationConstants.*;

public final class SearchClientConfig {

    public static final String INITIAL_LOG_MESSAGE = "Connecting to Elasticsearch at {}";

    private static final AWSCredentialsProvider credentialsProvider = new DefaultAWSCredentialsProviderChain();
    private static final Logger logger = LoggerFactory.getLogger(SearchClientConfig.class);
    public static final String SEARCH_INFRASTRUCTURE_CREDENTIALS = "SearchInfrastructureCredentials";

    private SearchClientConfig() {

    }

    public static SearchClient defaultSearchClient() {
        return new SearchClient(defaultRestHighLevelClientWrapper());
    }

    public static RestHighLevelClientWrapper defaultRestHighLevelClientWrapper() {
        return createElasticsearchClientWithInterceptor(SEARCH_INFRASTRUCTURE_API_URI);
    }

    public static RestHighLevelClientWrapper createElasticsearchClientWithInterceptor(String address) {
        logger.info(INITIAL_LOG_MESSAGE, address);

        var secretsReader = new SecretsReader();
        var credentials
                = secretsReader.fetchClassSecret(SEARCH_INFRASTRUCTURE_CREDENTIALS, Credentials.class);
        var uri = URI.create(SEARCH_INFRASTRUCTURE_AUTH_URI);

        var cognitoCredentials = new CognitoCredentials(credentials::getUsername, credentials::getPassword, uri);

        HttpRequestInterceptor interceptor =
                new CognitoInterceptor(cognitoCredentials);

        RestClientBuilder clientBuilder = RestClient
                .builder(HttpHost.create(address))
                .setHttpClientConfigCallback(config -> config.addInterceptorLast(interceptor));
        return new RestHighLevelClientWrapper(clientBuilder);
    }

}
