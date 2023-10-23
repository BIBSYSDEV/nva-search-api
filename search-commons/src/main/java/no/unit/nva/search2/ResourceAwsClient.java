package no.unit.nva.search2;

import static java.net.HttpURLConnection.HTTP_OK;
import static java.util.Objects.nonNull;
import static no.unit.nva.auth.AuthorizedBackendClient.AUTHORIZATION_HEADER;
import static no.unit.nva.commons.json.JsonUtils.singleLineObjectMapper;
import static no.unit.nva.search.constants.ApplicationConstants.RESOURCES_AGGREGATIONS;
import static no.unit.nva.search.utils.UriRetriever.ACCEPT;
import static no.unit.nva.search2.constant.ApplicationConstants.ALL;
import static no.unit.nva.search2.constant.ApplicationConstants.ASTERISK;
import static no.unit.nva.search2.constant.ApplicationConstants.COLON;
import static no.unit.nva.search2.constant.ApplicationConstants.COMMA;
import static no.unit.nva.search2.constant.ApplicationConstants.ZERO;
import static no.unit.nva.search2.model.ResourceParameterKey.FIELDS;
import static no.unit.nva.search2.model.ResourceParameterKey.FROM;
import static no.unit.nva.search2.model.ResourceParameterKey.SEARCH_AFTER;
import static no.unit.nva.search2.model.ResourceParameterKey.SEARCH_ALL;
import static no.unit.nva.search2.model.ResourceParameterKey.SIZE;
import static no.unit.nva.search2.model.ResourceParameterKey.SORT;
import static nva.commons.core.attempt.Try.attempt;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpResponse.BodyHandler;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Collection;
import java.util.Objects;
import java.util.stream.Stream;

import com.google.common.net.MediaType;
import no.unit.nva.search.CachedJwtProvider;
import no.unit.nva.search2.model.OpenSearchClient;
import no.unit.nva.search2.model.OpenSearchSwsResponse;
import no.unit.nva.search2.model.ParameterKey.FieldOperator;
import no.unit.nva.search2.model.QueryBuilderSourceWrapper;
import no.unit.nva.search2.model.QueryBuilderWrapper;
import no.unit.nva.search2.model.ResourceParameterKey;
import no.unit.nva.search2.model.ResourceSortKeys;
import nva.commons.core.JacocoGenerated;
import nva.commons.secrets.SecretsReader;
import org.jetbrains.annotations.NotNull;
import org.opensearch.common.collect.Tuple;
import org.opensearch.index.query.AbstractQueryBuilder;
import org.opensearch.index.query.BoolQueryBuilder;
import org.opensearch.index.query.MultiMatchQueryBuilder;
import org.opensearch.index.query.Operator;
import org.opensearch.index.query.QueryBuilders;
import org.opensearch.index.query.RangeQueryBuilder;
import org.opensearch.search.builder.SearchSourceBuilder;
import org.opensearch.search.sort.SortOrder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ResourceAwsClient implements OpenSearchClient<OpenSearchSwsResponse, ResourceAwsQuery> {

    private static final Logger logger = LoggerFactory.getLogger(ResourceAwsClient.class);
    private final CachedJwtProvider jwtProvider;
    private final HttpClient httpClient;
    private final BodyHandler<String> bodyHandler;

    private static final Integer SINGLE_FIELD = 1;

    public ResourceAwsClient(CachedJwtProvider cachedJwtProvider, HttpClient client) {
        super();
        this.jwtProvider = cachedJwtProvider;
        this.httpClient = client;
        this.bodyHandler = HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8);
    }

    @JacocoGenerated
    public static ResourceAwsClient defaultClient() {
        var cachedJwtProvider =
            OpenSearchClient.getCachedJwtProvider(new SecretsReader());

        return new ResourceAwsClient(cachedJwtProvider, HttpClient.newHttpClient());
    }

    @Override
    public OpenSearchSwsResponse doSearch(ResourceAwsQuery query) {
        return
            createQueryBuilderStream(query)
                .map(this::populateSearchRequest)
                .map(this::createRequest)
                .map(this::fetch)
                .map(this::handleResponse)
                .findFirst().orElseThrow();
    }

    private Stream<QueryBuilderWrapper> createQueryBuilderStream(ResourceAwsQuery query) {
        AbstractQueryBuilder<?> queryBuilder;
        if (query.isPresent(SEARCH_ALL)) {
            queryBuilder = multiMatchQuery(query);
        } else if (query.hasNoSearchValue()) {
            queryBuilder = QueryBuilders.matchAllQuery();
        } else {
            queryBuilder = boolQuery(query);
        }

        return Stream.of(new QueryBuilderWrapper(queryBuilder, query));
    }

    private QueryBuilderSourceWrapper populateSearchRequest(QueryBuilderWrapper queryBuilderWrapper) {
        var builder = new SearchSourceBuilder().query(queryBuilderWrapper.builder());
        var query = queryBuilderWrapper.query();
        var searchAfter = query.removeValue(SEARCH_AFTER);

        if (nonNull(searchAfter)) {
            var sortKeys = searchAfter.split(COMMA);
            builder.searchAfter(sortKeys);
        }

        if (isFirstPage(query)) {
            RESOURCES_AGGREGATIONS.forEach(builder::aggregation);
        }

        builder.size(query.getValue(SIZE).as());
        builder.from(query.getValue(FROM).as());
        getSortStream(query).forEach(orderTuple -> builder.sort(orderTuple.v1(), orderTuple.v2()));

        return new QueryBuilderSourceWrapper(builder, query.openSearchUri());
    }

    @JacocoGenerated
    private HttpRequest createRequest(QueryBuilderSourceWrapper qbs) {
        logger.info(qbs.source().query().toString());
        return HttpRequest
                   .newBuilder(qbs.requestUri())
                   .headers(
                       ACCEPT, MediaType.JSON_UTF_8.toString(),
                       AUTHORIZATION_HEADER, jwtProvider.getValue().getToken())
                   .POST(HttpRequest.BodyPublishers.ofString(qbs.source().toString())).build();
    }

    private HttpResponse<String> fetch(HttpRequest httpRequest) {
        return attempt(() -> httpClient.send(httpRequest, bodyHandler)).orElseThrow();
    }

    @JacocoGenerated
    private OpenSearchSwsResponse handleResponse(HttpResponse<String> response) {
        if (response.statusCode() != HTTP_OK) {
            throw new RuntimeException(response.body());
        }
        return attempt(() -> singleLineObjectMapper.readValue(response.body(), OpenSearchSwsResponse.class))
                   .orElseThrow();
    }

    private MultiMatchQueryBuilder multiMatchQuery(ResourceAwsQuery query) {
        var field = query.getValue(FIELDS).toString();
        return QueryBuilders
                   .multiMatchQuery(query.getValue(SEARCH_ALL).toString(), extractFields(field))
                   .operator(Operator.AND);
    }

    private BoolQueryBuilder boolQuery(ResourceAwsQuery query) {
        var bq = QueryBuilders.boolQuery();
        query.getOpenSearchParameters()
            .forEach((key, value) -> {
                var searchFields = key.searchFields().toArray(String[]::new);
                if (hasMultipleFields(searchFields)) {
                    bq.must(QueryBuilders.multiMatchQuery(value, searchFields));
                } else {
                    var searchField = searchFields[0];
                    if (isRangeQuery(key)) {
                        bq.must(rangeQuery(key.searchOperator(), searchField, value));
                    } else {
                        bq.must(QueryBuilders.matchQuery(searchField, value));
                    }
                }
            });
        return bq;
    }

    private RangeQueryBuilder rangeQuery(FieldOperator operator, String fieldName, String value) {
        return switch (operator) {
            case NONE, EQUALS -> throw new IllegalArgumentException("Operator not supported");
            case GREATER_THAN -> QueryBuilders.rangeQuery(fieldName).gt(value);
            case GREATER_THAN_OR_EQUAL_TO -> QueryBuilders.rangeQuery(fieldName).gte(value);
            case LESS_THAN -> QueryBuilders.rangeQuery(fieldName).lt(value);
            case LESS_THAN_OR_EQUAL_TO -> QueryBuilders.rangeQuery(fieldName).lte(value);
        };
    }

    @NotNull
    private String[] extractFields(String field) {
        return ALL.equals(field) || Objects.isNull(field)
                   ? ASTERISK.split(COMMA)
                   : Arrays.stream(field.split(COMMA))
                         .map(ResourceParameterKey::keyFromString)
                         .map(ResourceParameterKey::searchFields)
                         .flatMap(Collection::stream)
                         .toArray(String[]::new);
    }

    @JacocoGenerated
    private Tuple<String, SortOrder> expandSortKeys(String... strings) {
        var sortOrder = strings.length == 2 ? SortOrder.fromString(strings[1]) : SortOrder.ASC;
        var fieldName = ResourceSortKeys.fromSortKey(strings[0]).getFieldName();
        return new Tuple<>(fieldName, sortOrder);
    }

    @NotNull
    private Stream<Tuple<String, SortOrder>> getSortStream(ResourceAwsQuery query) {
        return Arrays.stream(query.getValue(SORT).<String>as().split(COMMA))
                   .map(sort -> sort.split(COLON))
                   .map(this::expandSortKeys);
    }

    private boolean isFirstPage(ResourceAwsQuery query) {
        return ZERO.equals(query.getValue(FROM).toString());
    }

    private static boolean hasMultipleFields(String... swsKey) {
        return swsKey.length > SINGLE_FIELD;
    }

    private static boolean isRangeQuery(ResourceParameterKey key) {
        return key.searchOperator() != FieldOperator.EQUALS;
    }
}
