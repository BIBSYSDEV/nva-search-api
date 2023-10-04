package no.unit.nva.search2;

import static com.amazonaws.auth.internal.SignerConstants.AUTHORIZATION;
import static java.util.Objects.nonNull;
import static no.unit.nva.search.constants.ApplicationConstants.RESOURCES_AGGREGATIONS;
import static no.unit.nva.search.constants.ApplicationConstants.SEARCH_INFRASTRUCTURE_API_URI;
import static no.unit.nva.search2.constant.ApplicationConstants.COLON;
import static no.unit.nva.search2.constant.ApplicationConstants.COMMA;
import static no.unit.nva.search2.constant.ApplicationConstants.RESOURCES;
import static no.unit.nva.search2.model.ResourceParameterKey.FIELDS;
import static no.unit.nva.search2.model.ResourceParameterKey.FROM;
import static no.unit.nva.search2.model.ResourceParameterKey.SEARCH_AFTER;
import static no.unit.nva.search2.model.ResourceParameterKey.SIZE;
import static no.unit.nva.search2.model.ResourceParameterKey.SORT;
import java.io.Closeable;
import java.io.IOException;
import java.util.Arrays;
import java.util.stream.Stream;
import no.unit.nva.search.CachedJwtProvider;
import no.unit.nva.search2.model.OpenSearchClient;
import no.unit.nva.search2.model.SortKeys;
import nva.commons.core.JacocoGenerated;
import nva.commons.secrets.SecretsReader;
import org.apache.http.HttpHost;
import org.opensearch.action.search.SearchRequest;
import org.opensearch.action.search.SearchResponse;
import org.opensearch.client.RequestOptions;
import org.opensearch.client.RestClient;
import org.opensearch.client.RestHighLevelClient;
import org.opensearch.common.collect.Tuple;
import org.opensearch.index.query.Operator;
import org.opensearch.index.query.QueryBuilders;
import org.opensearch.index.query.QueryStringQueryBuilder;
import org.opensearch.search.builder.SearchSourceBuilder;
import org.opensearch.search.sort.SortOrder;
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
        return
            getQueryBuilderStream(query)
                .map(this::searchSourceWithAggregation)
                .map(this::searchRequest)
                .map(this::searchResponse)
                .findFirst().orElseThrow();
    }

    private static Stream<Tuple<QueryStringQueryBuilder, ResourceAwsQuery>> getQueryBuilderStream(
        ResourceAwsQuery query) {
        var luceneParameters = query.toLuceneParameter().get("q");
        var stringQueryBuilder = QueryBuilders.queryStringQuery(luceneParameters);
        stringQueryBuilder.defaultOperator(Operator.AND);
        var fields = query.removeValue(FIELDS);
        if (nonNull(fields)) {
            Arrays.stream(fields.split(COMMA)).forEach(stringQueryBuilder::field);
        }
        return Stream.of(new Tuple<>(stringQueryBuilder, query));
    }

    @Override
    public void close() throws IOException {
        client.close();
    }

    private SearchSourceBuilder searchSourceWithAggregation(Tuple<QueryStringQueryBuilder, ResourceAwsQuery> tuple) {
        var builder = new SearchSourceBuilder().query(tuple.v1());
        var query = tuple.v2();
        var searchAfter = query.removeValue(SEARCH_AFTER);
        if (nonNull(searchAfter)) {
            var sortKeys = searchAfter.split(COMMA);
            builder.searchAfter(sortKeys);
        }
        RESOURCES_AGGREGATIONS.forEach(builder::aggregation);
        builder.size(query.getValue(SIZE).as());
        builder.from(query.getValue(FROM).as());
        Arrays.stream(query.getValue(SORT).<String>as().split(COMMA))
            .map(sort -> sort.split(COLON))
            .map(this::expandSortKeys)
            .forEach(params -> builder.sort(params.v1(), params.v2()));
        return builder;
    }

    private Tuple<String, SortOrder> expandSortKeys(String... strings) {
        var sortOrder = strings.length == 2 ? SortOrder.fromString(strings[1]) : SortOrder.ASC;
        var luceneKey = SortKeys.keyFromString(strings[0]).getLuceneField();
        return new Tuple<>(luceneKey, sortOrder);
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
