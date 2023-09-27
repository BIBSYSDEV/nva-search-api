package no.unit.nva.search2;

import static com.amazonaws.auth.internal.SignerConstants.AUTHORIZATION;
import static no.unit.nva.search.constants.ApplicationConstants.RESOURCES_AGGREGATIONS;
import static no.unit.nva.search.constants.ApplicationConstants.SEARCH_INFRASTRUCTURE_API_URI;
import static no.unit.nva.search2.constant.ApplicationConstants.RESOURCES;
import static no.unit.nva.search2.model.ResourceParameterKey.FROM;
import static no.unit.nva.search2.model.ResourceParameterKey.SIZE;
import static no.unit.nva.search2.model.ResourceParameterKey.SORT;
import java.io.Closeable;
import java.io.IOException;
import java.util.stream.Stream;
import no.unit.nva.search.CachedJwtProvider;
import no.unit.nva.search2.model.OpenSearchClient;
import nva.commons.core.JacocoGenerated;
import nva.commons.secrets.SecretsReader;
import org.apache.http.HttpHost;
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

@SuppressWarnings("PMD.CloseResource")
public class OpenSearchAwsClient implements OpenSearchClient<SearchResponse, ResourceAwsQuery>, Closeable {

    private static final HttpHost httpHost = HttpHost.create(SEARCH_INFRASTRUCTURE_API_URI);
    private static final Logger logger = LoggerFactory.getLogger(OpenSearchAwsClient.class);
    private final CachedJwtProvider jwtProvider;
    private final RestHighLevelClient client;

    public OpenSearchAwsClient(CachedJwtProvider cachedJwtProvider, RestHighLevelClient client) {
        super();
        this.jwtProvider = cachedJwtProvider;
        this.client = client;
    }

    @JacocoGenerated
    public static OpenSearchAwsClient defaultClient() {
        var cachedJwtProvider =
            OpenSearchClient
                .getCachedJwtProvider(new SecretsReader());
        var client = new RestHighLevelClient(
            RestClient.builder(httpHost)
        );

        return new OpenSearchAwsClient(cachedJwtProvider, client);
    }

    @Override
    public SearchResponse doSearch(ResourceAwsQuery query, String mediaType) {
        var luceneParameters = query.toLuceneParameter().get("q");
        return
            Stream.of(QueryBuilders.queryStringQuery(luceneParameters))
                .map(this::searchSource)
                .map(searchSource -> searchSource
                    .size(query.getValue(SIZE).as())
                    .from(query.getValue(FROM).as())
                    .sort(query.getValue(SORT).<String>as()))
                .map(this::searchRequest)
                .map(this::searchResponse)
                .findFirst().orElseThrow();
    }

    @Override
    public void close() throws IOException {
        client.close();
    }

    private SearchSourceBuilder searchSource(QueryStringQueryBuilder queryBuilder) {
        var builder = new SearchSourceBuilder().query(queryBuilder);
        RESOURCES_AGGREGATIONS.forEach(builder::aggregation);
        return builder;
    }

    private SearchRequest searchRequest(SearchSourceBuilder sourceBuilder) {
        return new SearchRequest(RESOURCES).source(sourceBuilder);
    }


    @JacocoGenerated
    private SearchResponse searchResponse(SearchRequest searchRequest) {
        try (client) {
            return client.search(searchRequest, getRequestOptions());
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
        }
        return (SearchResponse) Stream.empty();
    }

    private RequestOptions getRequestOptions() {
        var token = "Bearer " + jwtProvider.getValue().getToken();
        return RequestOptions.DEFAULT
                   .toBuilder()
                   .addHeader(AUTHORIZATION, token)
                   .build();
    }
}
