package no.unit.nva.search2;

import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;
import static no.unit.nva.commons.json.JsonUtils.dtoObjectMapper;
import static no.unit.nva.search.constants.ApplicationConstants.objectMapperWithEmpty;
import static no.unit.nva.search.models.SearchResponseDto.formatAggregations;
import static no.unit.nva.search2.constant.Defaults.DEFAULT_OFFSET;
import static no.unit.nva.search2.constant.Defaults.DEFAULT_VALUE_PER_PAGE;
import static no.unit.nva.search2.constant.Defaults.DEFAULT_VALUE_SORT;
import static no.unit.nva.search2.constant.Defaults.DEFAULT_VALUE_SORT_ORDER;
import static no.unit.nva.search2.model.ResourceParameterKey.FROM;
import static no.unit.nva.search2.model.ResourceParameterKey.PAGE;
import static no.unit.nva.search2.model.ResourceParameterKey.SEARCH_AFTER;
import static no.unit.nva.search2.model.ResourceParameterKey.SIZE;
import static no.unit.nva.search2.model.ResourceParameterKey.SORT;
import static no.unit.nva.search2.model.ResourceParameterKey.VALID_LUCENE_PARAMETER_KEYS;
import static no.unit.nva.search2.model.ResourceParameterKey.keyFromString;
import static nva.commons.core.attempt.Try.attempt;
import static org.apache.http.entity.ContentType.APPLICATION_JSON;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.io.IOException;
import java.net.URI;
import java.util.Arrays;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import no.unit.nva.search2.model.OpenSearchQuery;
import no.unit.nva.search2.model.OpenSearchQueryBuilder;
import no.unit.nva.search2.model.PagedSearchResourceDto;
import no.unit.nva.search2.model.ResourceParameterKey;
import nva.commons.apigateway.exceptions.ApiGatewayException;
import nva.commons.core.paths.UriWrapper;
import org.jetbrains.annotations.NotNull;
import org.opensearch.action.search.SearchResponse;
import org.opensearch.common.xcontent.XContentHelper;
import org.opensearch.common.xcontent.XContentType;
import org.opensearch.search.SearchHit;

public final class ResourceAwsQuery extends OpenSearchQuery<ResourceParameterKey> {

    private ResourceAwsQuery() {
        super();
    }

    public PagedSearchResourceDto doSearch(OpenSearchAwsClient queryClient) throws ApiGatewayException {
        return
            Stream.of(queryClient.doSearch(this, APPLICATION_JSON.toString()))
                .map(this::toResponse)
                .findFirst().orElseThrow();
    }

    @NotNull
    private PagedSearchResourceDto toResponse(@NotNull SearchResponse response) {

        final var offset = this.getValue(FROM).as(Long.class);
        final var url = gatewayUri.toString().split("\\?")[0];
        final var requestParameter = toGateWayRequestParameter();
        final var id = createUriOffsetRef(url, requestParameter, offset);
        var nextResults
            = hasMoreResults(response)
            ? createUriOffsetRef(url, requestParameter, offset + getQuerySize())
            : null;
        var nextResultsBySortKey
            = hasMoreResults(response)
            ? getNextResultsBySortKey(response, requestParameter, url)
            : null;

        var hasPreviousResults = offset > 0;
        var previousResults
            = hasPreviousResults
            ? createUriOffsetRef(url, requestParameter, offset - getQuerySize())
            : null;

        var hits = Arrays.stream(response.getHits().getHits())
                       .map(hitsToJsonString())
                       .map(jsonToNode())
                       .toList();
        var aggregations = extractAggregations(response);

        return PagedSearchResourceDto.Builder.builder()
            .withTotalHits(response.getHits().getTotalHits().value)
            .withHits(hits)
            .withAggregations(aggregations)
            .withId(id)
            .withNextResults(nextResults)
            .withPreviousResults(previousResults)
            .withNextResultsBySortKey(nextResultsBySortKey)
            .build();
    }

    private boolean hasMoreResults(@NotNull SearchResponse response) {
        final var offset = this.getValue(FROM).as(Long.class);
        return offset < response.getHits().getTotalHits().value;
    }

    @NotNull
    private static Function<String, JsonNode> jsonToNode() {
        return content -> {
            try {
                return dtoObjectMapper.readTree(content);
            } catch (JsonProcessingException e) {
                throw new RuntimeException(e);
            }
        };
    }

    private static Function<SearchHit,String> hitsToJsonString() {
        return hit -> {
            try {
                return XContentHelper
                           .convertToJson(
                               hit.getSourceRef(),
                               true,
                               false,
                               XContentType.JSON);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        };
    }

    private static JsonNode extractAggregations(SearchResponse searchResponse) {
        var json = attempt(() -> objectMapperWithEmpty.readTree(searchResponse.toString())).orElseThrow();
        var aggregations = (ObjectNode) json.get("aggregations");
        return  nonNull(aggregations) ? formatAggregations(aggregations) : null;
    }

    private URI getNextResultsBySortKey(
        @NotNull SearchResponse response, Map<String, String> requestParameter, String url
    ) {
        requestParameter.remove(FROM.key());
        var lastIndex = response.getHits().getHits().length - 1;
        var sortedP =
            Arrays.stream(response.getHits().getHits()[lastIndex].getSortValues())
                .map(Object::toString)
                .collect(Collectors.joining(","));

        requestParameter.put(SEARCH_AFTER.key(), sortedP);
        return UriWrapper.fromUri(url)
                   .addQueryParameters(requestParameter)
                   .getUri();
    }


    private URI createUriOffsetRef(String source, Map<String, String> params, Long offset) {
        if (offset < 0) {
            return null;
        }
        params.put(FROM.key(), String.valueOf(offset));
        return UriWrapper.fromUri(source)
                   .addQueryParameters(params)
                   .getUri();
    }



    Long getQuerySize() {
        return getValue(SIZE).as(Long.class);
    }


    public static final class Builder
        extends OpenSearchQueryBuilder<ResourceParameterKey,ResourceAwsQuery> {

        public static final String ALL = "all";

        private Builder() {
            super(new ResourceAwsQuery());
        }

        public static ResourceAwsQuery.Builder queryBuilder() {
            return new Builder();
        }

        @Override
        protected void assignDefaultValues() {
            requiredMissing().forEach(key -> {
                switch (key) {
                    case FROM -> setValue(key.key(), DEFAULT_OFFSET);
                    case SIZE -> setValue(key.key(), DEFAULT_VALUE_PER_PAGE);
                    case SORT -> setValue(key.key(), DEFAULT_VALUE_SORT + ":" + DEFAULT_VALUE_SORT_ORDER);
                    default -> {
                    }
                }
            });
        }

        @Override
        protected void setValue(String key, String value) {
            var qpKey = keyFromString(key);
            switch (qpKey) {
                case SEARCH_AFTER,
                         FROM,
                         SIZE,
                         PAGE -> query.setQueryValue(qpKey, value);
                case FIELDS -> query.setQueryValue(qpKey, expandFields(value));
                case SORT -> setSortQuery(qpKey, value);
                case SORT_ORDER -> addSortOrderToSortQuery(value);
                case CATEGORY, CONTRIBUTOR,
                         CREATED_BEFORE, CREATED_SINCE,
                         DOI, FUNDING, FUNDING_SOURCE, ID,
                         INSTITUTION, ISSN,
                         MODIFIED_BEFORE, MODIFIED_SINCE,
                         PROJECT_CODE, PUBLISHED_BEFORE,
                         PUBLISHED_SINCE, SEARCH_ALL, TITLE,
                         UNIT, USER, YEAR_REPORTED -> query.setLucineValue(qpKey, value);
                case LANG -> {
                    // ignore and continue
                }
                default -> invalidKeys.add(key);
            }
        }

        private void addSortOrderToSortQuery(String value) {
            query.setQueryValue(SORT, mergeParameters(query.getValue(SORT).toString(), value));
        }

        @Override
        protected void applyRulesAfterValidation() {
            // convert page to offset if offset is not set
            if (nonNull(query.getValue(PAGE))) {
                if (isNull(query.getValue(FROM))) {
                    var page = query.getValue(PAGE).as(Integer.TYPE);
                    var perPage = query.getValue(SIZE).as(Integer.TYPE);
                    query.setQueryValue(FROM, String.valueOf(page * perPage));
                }
                query.removeValue(PAGE);
            }
            // TODO check if field is set and has value 'all' then populate with all fields
        }

        private String expandFields(String value) {
            return ALL.equals(value)
                       ? String.join("|", VALID_LUCENE_PARAMETER_KEYS.stream().map(ResourceParameterKey::key).toList())
                       : value;
        }

        private  void setSortQuery(ResourceParameterKey qpKey, String value) {
            var validFieldValue =  decodeUTF(value).replaceAll(" (asc|desc)", ":$1");
            query.setQueryValue(qpKey, mergeParameters(query.getValue(qpKey).as(), validFieldValue));
        }
    }

}