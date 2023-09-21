package no.unit.nva.search2.aws;

import no.unit.nva.search.CachedJwtProvider;
import no.unit.nva.search.CognitoAuthenticator;
import no.unit.nva.search2.model.OpenSearchClient;
import no.unit.nva.search2.model.OpenSearchQuery;
import no.unit.nva.search2.sws.OpenSearchSwsClient;
import nva.commons.core.JacocoGenerated;
import nva.commons.secrets.SecretsReader;
import org.opensearch.action.search.SearchRequest;
import org.opensearch.action.search.SearchResponse;
import org.opensearch.client.RequestOptions;
import org.opensearch.client.RestClient;
import org.opensearch.client.RestHighLevelClient;
import org.opensearch.index.query.QueryBuilders;
import org.opensearch.index.query.QueryStringQueryBuilder;
import org.opensearch.search.builder.SearchSourceBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.http.HttpClient;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.stream.Stream;

import static no.unit.nva.search.constants.ApplicationConstants.RESOURCES_AGGREGATIONS;
import static no.unit.nva.search2.common.ResourceParameterKey.FROM;
import static no.unit.nva.search2.common.ResourceParameterKey.SIZE;
import static no.unit.nva.search2.common.ResourceParameterKey.SORT;

public class OpenSearchAwsClient implements OpenSearchClient<SearchResponse, ResourceQuery2> {
    private static final Logger logger = LoggerFactory.getLogger(OpenSearchSwsClient.class);
    private static final String REQUESTING_SEARCH_FROM = "OpenSearchSwsClient url -> {}";
    private final CachedJwtProvider jwtProvider;
    private final HttpClient httpClient;
    private final HttpResponse.BodyHandler<String> bodyHandler;

    public OpenSearchAwsClient(CachedJwtProvider cachedJwtProvider, HttpClient httpClient) {
        this.bodyHandler = HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8);
        this.jwtProvider = cachedJwtProvider;
        this.httpClient = httpClient;
    }
    @JacocoGenerated
    public static OpenSearchAwsClient defaultClient() {
        var cachedJwtProvider =
            OpenSearchClient.getUsernamePasswordStream(new SecretsReader())
                .map(OpenSearchClient::getCognitoCredentials)
                .map(CognitoAuthenticator::prepareWithCognitoCredentials)
                .map(CachedJwtProvider::prepareWithAuthenticator)
                .findFirst().orElseThrow();
        return new OpenSearchAwsClient(cachedJwtProvider, HttpClient.newHttpClient());
    }

    @Override
    public SearchResponse doSearch(ResourceQuery2 query, String mediaType) {
        var luceneParameters = query.toLuceneParameter().get("q");
        return
            Stream.of(QueryBuilders.queryStringQuery(luceneParameters))
                .map(qBuilder -> qBuilder.allowLeadingWildcard(true))
                .map(OpenSearchAwsClient::searchSource)
                .map(searchSource -> searchSource
                    .size(GetSize(query))
                    .from(getFrom(query))
                    .sort(query.getValue(SORT)))
                .map(OpenSearchAwsClient::searchRequest)
                .map(OpenSearchAwsClient::searchResponse)
                .findFirst().orElseThrow();
    }



    private static int getFrom(OpenSearchQuery<?,?> query) {
        return Integer.parseInt(query.getValue(FROM));
    }

    private static int GetSize(OpenSearchQuery<?,?> query) {
        return Integer.parseInt(query.getValue(SIZE));
    }

    private static SearchSourceBuilder searchSource(QueryStringQueryBuilder queryBuilder) {
        var builder = new SearchSourceBuilder().query(queryBuilder);
            RESOURCES_AGGREGATIONS.forEach(builder::aggregation);
        return builder;
    }

    private static SearchRequest searchRequest(SearchSourceBuilder sourceBuilder) {
        return new SearchRequest("your-index-name").source(sourceBuilder);
    }

    private static SearchResponse searchResponse(SearchRequest searchRequest) {
        var requestOptions = RequestOptions.DEFAULT;
        try (RestHighLevelClient client = new RestHighLevelClient(
            RestClient.builder("your-opensearch-host:9200")
        )) {
            return client.search(searchRequest, requestOptions);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

}
