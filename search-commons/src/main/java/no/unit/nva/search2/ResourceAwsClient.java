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
import static no.unit.nva.search2.constant.ErrorMessages.OPERATOR_NOT_SUPPORTED;
import static no.unit.nva.search2.model.ParameterKey.escapeSearchString;
import static no.unit.nva.search2.model.ResourceParameterKey.CONTRIBUTOR_ID;
import static no.unit.nva.search2.model.ResourceParameterKey.FIELDS;
import static no.unit.nva.search2.model.ResourceParameterKey.FROM;
import static no.unit.nva.search2.model.ResourceParameterKey.SEARCH_AFTER;
import static no.unit.nva.search2.model.ResourceParameterKey.SEARCH_ALL;
import static no.unit.nva.search2.model.ResourceParameterKey.SIZE;
import static no.unit.nva.search2.model.ResourceParameterKey.SORT;
import static nva.commons.core.attempt.Try.attempt;
import com.google.common.net.MediaType;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpResponse.BodyHandler;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.stream.Stream;
import no.unit.nva.search.CachedJwtProvider;
import no.unit.nva.search2.model.OpenSearchClient;
import no.unit.nva.search2.model.OpenSearchSwsResponse;
import no.unit.nva.search2.model.ParameterKey;
import no.unit.nva.search2.model.QueryBuilderSourceWrapper;
import no.unit.nva.search2.model.QueryBuilderWrapper;
import no.unit.nva.search2.model.ResourceParameterKey;
import no.unit.nva.search2.model.ResourceSortKeys;
import nva.commons.core.JacocoGenerated;
import nva.commons.secrets.SecretsReader;
import org.jetbrains.annotations.NotNull;
import org.opensearch.common.collect.Tuple;
import org.opensearch.index.query.BoolQueryBuilder;
import org.opensearch.index.query.MultiMatchQueryBuilder;
import org.opensearch.index.query.MultiMatchQueryBuilder.Type;
import org.opensearch.index.query.Operator;
import org.opensearch.index.query.QueryBuilder;
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
    private final UserSettingsClient userSettingsClient;

    private static final Integer SINGLE_FIELD = 1;

    public ResourceAwsClient(CachedJwtProvider cachedJwtProvider, HttpClient client) {
        super();
        this.jwtProvider = cachedJwtProvider;
        this.httpClient = client;
        this.bodyHandler = HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8);
        this.userSettingsClient = new UserSettingsClient(cachedJwtProvider, client);
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
        var queryBuilder = query.hasNoSearchValue()
                ? QueryBuilders.matchAllQuery()
                : boolQuery(query);
        return Stream.of(new QueryBuilderWrapper(queryBuilder, query));
    }

    private QueryBuilderSourceWrapper populateSearchRequest(QueryBuilderWrapper queryBuilderWrapper) {
        var builder = new SearchSourceBuilder().query(queryBuilderWrapper.builder());
        var query = queryBuilderWrapper.query();
        var searchAfter = query.removeKey(SEARCH_AFTER);

        if (nonNull(searchAfter)) {
            var sortKeys = searchAfter.split(COMMA);
            builder.searchAfter(sortKeys);
        }

        if (isFirstPage(query)) {
            RESOURCES_AGGREGATIONS.forEach(builder::aggregation);
        }

        builder.size(query.getValue(SIZE).as());
        builder.from(query.getValue(FROM).as());
        getSortStream(query)
            .forEach(orderTuple -> builder.sort(orderTuple.v1(), orderTuple.v2()));

        return new QueryBuilderSourceWrapper(builder, query.getOpenSearchUri());
    }

    @JacocoGenerated
    private HttpRequest createRequest(QueryBuilderSourceWrapper qbs) {
        logger.info(qbs.source().query().toString());
        return HttpRequest
                   .newBuilder(qbs.requestUri())
                   .headers(
                       ACCEPT, MediaType.JSON_UTF_8.toString(),
                       "Content-Type", MediaType.JSON_UTF_8.toString(),
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

    /**
     * Creates a boolean query, with all the search parameters.
     * @param query ResourceAwsQuery
     * @return a BoolQueryBuilder
     */
    @SuppressWarnings({"PMD.SwitchStmtsShouldHaveDefault"})
    private BoolQueryBuilder boolQuery(ResourceAwsQuery query) {
        var bq = QueryBuilders.boolQuery();
        query.getOpenSearchParameters()
            .forEach((key, value) -> {
                if (key.equals(SEARCH_ALL)) {
                    bq.must(multiMatchQuery(query));
                } else if (key.fieldType().equals(ParameterKey.ParamKind.KEYWORD)) {
                    addKeywordQuery(key, value, bq);
                } else {
                    switch (key.searchOperator()) {
                        case MUST -> bq.must(buildQuery(key, value));
                        case MUST_NOT -> bq.mustNot(buildQuery(key, value));
                        case SHOULD -> bq.should(buildQuery(key, value));
                        case GREATER_THAN_OR_EQUAL_TO, LESS_THAN -> bq.must(rangeQuery(key, value));
                    }
                }
                if (key.equals(CONTRIBUTOR_ID)) {
                    addPromotedQuery(query, bq);
                }
            });
        return bq;
    }

    /**
     * Creates a multi match query, all words needs to be present, within a document.
     * @param query ResourceAwsQuery
     * @return a MultiMatchQueryBuilder
     */
    private MultiMatchQueryBuilder multiMatchQuery(ResourceAwsQuery query) {
        var fields = extractFields(query.getValue(FIELDS).toString());
        var value = escapeSearchString(query.getValue(SEARCH_ALL).toString());
        return QueryBuilders
                   .multiMatchQuery(value, fields)
                   .type(Type.CROSS_FIELDS)
                   .operator(Operator.AND);
    }

    private void addPromotedQuery(ResourceAwsQuery query, BoolQueryBuilder bq) {
        var promotedPublications = userSettingsClient
            .doSearch(query)
            .promotedPublications();
        if (hasPromotedPublications(promotedPublications)) {
            query.removeKey(SORT);                              // remove sort to avoid messing up "sorting by score"
            for (int i = 0; i < promotedPublications.size(); i++) {
                bq.should(
                    QueryBuilders
                        .matchQuery("id", promotedPublications.get(i))
                        .boost(3.14F + promotedPublications.size() - i));
            }
        }
    }

    private void addKeywordQuery(ResourceParameterKey key, String value, BoolQueryBuilder bq) {
        final var searchFields = key.searchFields().toArray(String[]::new);
        final var values = Arrays.stream(value.split(COMMA)).map(String::trim).toArray(String[]::new);
        final var multipleFields = hasMultipleFields(searchFields);

        Arrays.stream(searchFields).forEach(searchField -> {
            final var termsQuery = QueryBuilders.termsQuery(searchField, values).boost(key.fieldBoost());
            switch (key.searchOperator()) {
                case MUST -> {
                    if (multipleFields) {
                        bq.should(termsQuery);
                    } else {
                        bq.must(termsQuery);
                    }
                }
                case MUST_NOT -> bq.mustNot(termsQuery);
                case SHOULD -> bq.should(termsQuery);
                default -> throw new IllegalArgumentException(OPERATOR_NOT_SUPPORTED);
            }
        });
    }

    private QueryBuilder buildQuery(ResourceParameterKey key, String value) {
        final var searchFields = key.searchFields().toArray(String[]::new);
        if (hasMultipleFields(searchFields)) {
            return QueryBuilders
                       .multiMatchQuery(escapeSearchString(value), searchFields)
                       .operator(operatorByKey(key));
        }
        var searchField = searchFields[0];
        return QueryBuilders
                   .matchQuery(searchField, escapeSearchString(value))
                   .boost(key.fieldBoost())
                   .operator(operatorByKey(key));
    }

    private RangeQueryBuilder rangeQuery(ResourceParameterKey key, String value) {
        final var searchField = key.searchFields().toArray()[0].toString();

        return switch (key.searchOperator()) {
            case MUST, MUST_NOT, SHOULD -> throw new IllegalArgumentException(OPERATOR_NOT_SUPPORTED);
            case GREATER_THAN_OR_EQUAL_TO -> QueryBuilders.rangeQuery(searchField).gte(value);
            case LESS_THAN -> QueryBuilders.rangeQuery(searchField).lt(value);
        };
    }

    private Operator operatorByKey(ResourceParameterKey key) {
        return switch (key.searchOperator()) {
            case MUST -> Operator.AND;
            case MUST_NOT, SHOULD -> Operator.OR;
            case GREATER_THAN_OR_EQUAL_TO, LESS_THAN -> throw new IllegalArgumentException(OPERATOR_NOT_SUPPORTED);
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
        return
            query.getOptional(SORT).stream()
                .flatMap(sort -> Arrays.stream(sort.split(COMMA)))
                .map(sort -> sort.split(COLON))
                .map(this::expandSortKeys);
    }

    private boolean isFirstPage(ResourceAwsQuery query) {
        return ZERO.equals(query.getValue(FROM).toString());
    }

    private boolean hasMultipleFields(String... swsKey) {
        return swsKey.length > SINGLE_FIELD;
    }

    private boolean hasPromotedPublications(List<String> promotedPublications) {
        return  nonNull(promotedPublications) && !promotedPublications.isEmpty();
    }

}
