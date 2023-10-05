package no.unit.nva.search2;

import static java.util.Objects.nonNull;
import static no.unit.nva.auth.AuthorizedBackendClient.AUTHORIZATION_HEADER;
import static no.unit.nva.auth.uriretriever.AuthorizedBackendUriRetriever.ACCEPT;
import static no.unit.nva.search.constants.ApplicationConstants.RESOURCES_AGGREGATIONS;
import static no.unit.nva.search2.constant.ApplicationConstants.COLON;
import static no.unit.nva.search2.constant.ApplicationConstants.COMMA;
import static no.unit.nva.search2.constant.ApplicationConstants.objectMapperWithEmpty;
import static no.unit.nva.search2.model.ResourceParameterKey.FIELDS;
import static no.unit.nva.search2.model.ResourceParameterKey.FROM;
import static no.unit.nva.search2.model.ResourceParameterKey.SEARCH_AFTER;
import static no.unit.nva.search2.model.ResourceParameterKey.SIZE;
import static no.unit.nva.search2.model.ResourceParameterKey.SORT;
import static nva.commons.core.attempt.Try.attempt;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpResponse.BodyHandler;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.stream.Stream;
import no.unit.nva.search.CachedJwtProvider;
import no.unit.nva.search2.model.OpenSearchClient;
import no.unit.nva.search2.model.OpenSearchSwsResponse;
import no.unit.nva.search2.model.QueryBuilder;
import no.unit.nva.search2.model.QueryBuilderSource;
import no.unit.nva.search2.model.SortKeys;
import nva.commons.apigateway.exceptions.BadGatewayException;
import nva.commons.core.JacocoGenerated;
import nva.commons.secrets.SecretsReader;
import org.apache.http.HttpStatus;
import org.opensearch.common.collect.Tuple;
import org.opensearch.index.query.Operator;
import org.opensearch.index.query.QueryBuilders;
import org.opensearch.search.builder.SearchSourceBuilder;
import org.opensearch.search.sort.SortOrder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class OpenSearchAwsClient implements OpenSearchClient<OpenSearchSwsResponse, ResourceAwsQuery> {

    private static final Logger logger = LoggerFactory.getLogger(OpenSearchAwsClient.class);
    private final CachedJwtProvider jwtProvider;
    private final HttpClient httpClient;
    private final BodyHandler<String> bodyHandler;

    public OpenSearchAwsClient(CachedJwtProvider cachedJwtProvider, HttpClient client) {
        super();
        this.jwtProvider = cachedJwtProvider;
        this.httpClient = client;
        this.bodyHandler = HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8);
    }

    @JacocoGenerated
    public static OpenSearchAwsClient defaultClient() {
        var cachedJwtProvider =
            OpenSearchClient
                .getCachedJwtProvider(new SecretsReader());
        var client = HttpClient.newHttpClient();

        return new OpenSearchAwsClient(cachedJwtProvider, client);
    }

    @Override
    public OpenSearchSwsResponse doSearch(ResourceAwsQuery query, String mediaType) {
        return
            getQueryBuilderStream(query, mediaType)
                .map(this::searchSourceWithAggregation)
                .map(this::httpRequest)
                .map(this::sendHttpRequest)
                .findFirst().orElseThrow();
    }

    private Stream<QueryBuilder> getQueryBuilderStream(ResourceAwsQuery query, String mediaType) {
        var luceneParameters = query.toLuceneParameter().get("q");
        var stringQueryBuilder = QueryBuilders.queryStringQuery(luceneParameters);
        stringQueryBuilder.defaultOperator(Operator.AND);
        var fields = query.removeValue(FIELDS);
        if (nonNull(fields)) {
            Arrays.stream(fields.split(COMMA)).forEach(stringQueryBuilder::field);
        }
        return Stream.of(new QueryBuilder(stringQueryBuilder, query, mediaType));
    }

    private QueryBuilderSource searchSourceWithAggregation(QueryBuilder queryBuilder) {
        var builder = new SearchSourceBuilder().query(queryBuilder.buider());
        var query = queryBuilder.query();
        var mediaType = queryBuilder.mediaType();
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

        return new QueryBuilderSource(builder, query.openSearchUri(), mediaType);
    }

    private Tuple<String, SortOrder> expandSortKeys(String... strings) {
        var sortOrder = strings.length == 2 ? SortOrder.fromString(strings[1]) : SortOrder.ASC;
        var luceneKey = SortKeys.keyFromString(strings[0]).getLuceneField();
        return new Tuple<>(luceneKey, sortOrder);
    }

    @JacocoGenerated
    private HttpRequest httpRequest(QueryBuilderSource qbs) {
        logger.info(qbs.requestUri().toString());
        return HttpRequest
                   .newBuilder(qbs.requestUri())
                   .headers(ACCEPT, qbs.mediaType(), AUTHORIZATION_HEADER, jwtProvider.getValue().getToken())
                   .POST(HttpRequest.BodyPublishers.ofString(qbs.source().toString())).build();
    }

    private OpenSearchSwsResponse sendHttpRequest(HttpRequest httpRequest) {
        return
            attempt(() -> httpClient.send(httpRequest, bodyHandler))
                .map(this::handleResponse)
                .orElseThrow();
    }

    @JacocoGenerated
    private OpenSearchSwsResponse handleResponse(HttpResponse<String> response) throws BadGatewayException {
        if (response.statusCode() != HttpStatus.SC_OK) {
            logger.error(response.body());
            throw new BadGatewayException(response.body());
        }
        return toSwsResponse(response);
    }

    private static OpenSearchSwsResponse toSwsResponse(HttpResponse<String> response) {
        return attempt(() -> objectMapperWithEmpty.readValue(response.body(), OpenSearchSwsResponse.class))
                   .orElseThrow();
    }
}
