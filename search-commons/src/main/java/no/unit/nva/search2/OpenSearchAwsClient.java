package no.unit.nva.search2;

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

import java.io.IOException;
import java.util.stream.Stream;

import static com.amazonaws.auth.internal.SignerConstants.AUTHORIZATION;
import static no.unit.nva.search.constants.ApplicationConstants.RESOURCES_AGGREGATIONS;
import static no.unit.nva.search.constants.ApplicationConstants.SEARCH_INFRASTRUCTURE_API_URI;
import static no.unit.nva.search2.constant.ApplicationConstants.RESOURCES;
import static no.unit.nva.search2.model.ResourceParameterKey.FROM;
import static no.unit.nva.search2.model.ResourceParameterKey.SIZE;
import static no.unit.nva.search2.model.ResourceParameterKey.SORT;

public class OpenSearchAwsClient implements OpenSearchClient<SearchResponse, ResourceAwsQuery> {

    private final CachedJwtProvider jwtProvider;
    private final HttpHost httpHost;

    public OpenSearchAwsClient(CachedJwtProvider cachedJwtProvider, HttpHost httpHost) {
        super();
        this.jwtProvider = cachedJwtProvider;
        this.httpHost = httpHost;
    }

    @JacocoGenerated
    public static OpenSearchAwsClient defaultClient() {
        var cachedJwtProvider =
            OpenSearchClient.getCachedJwtProvider(new SecretsReader());
        var host = HttpHost.create(SEARCH_INFRASTRUCTURE_API_URI);
        return new OpenSearchAwsClient(cachedJwtProvider, host);
    }

    @Override
    public SearchResponse doSearch(ResourceAwsQuery query, String mediaType) {
        var luceneParameters = query.toLuceneParameter().get("q");
        return
            Stream.of(QueryBuilders.queryStringQuery(luceneParameters))
                .map(this::searchSource)
                .map(searchSource -> searchSource
                    .size(query.getValue(SIZE).as(Integer.class))
                    .from(query.getValue(FROM).as(Integer.class))
                    .sort(query.getValue(SORT).as()))
                .map(this::searchRequest)
                .map(this::searchResponse)
                .findFirst().orElseThrow();
    }

    private SearchSourceBuilder searchSource(QueryStringQueryBuilder queryBuilder) {
        var builder = new SearchSourceBuilder().query(queryBuilder);
        RESOURCES_AGGREGATIONS.forEach(builder::aggregation);
        return builder;
    }

    private SearchRequest searchRequest(SearchSourceBuilder sourceBuilder) {
        return new SearchRequest(RESOURCES).source(sourceBuilder);
    }

    private SearchResponse searchResponse(SearchRequest searchRequest) {
        try (RestHighLevelClient client = new RestHighLevelClient(
            RestClient.builder(httpHost)
        )) {
            return client.search(searchRequest, getRequestOptions());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private RequestOptions getRequestOptions() {
        var token = "Bearer " + jwtProvider.getValue().getToken();
        return RequestOptions.DEFAULT
                   .toBuilder()
                   .addHeader(AUTHORIZATION, token)
                   .build();
    }
}
