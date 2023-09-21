package no.unit.nva.search2.aws;


import no.unit.nva.search2.common.ResourceParameterKey;
import no.unit.nva.search2.model.OpenSearchClient;
import no.unit.nva.search2.model.OpenSearchQuery;
import no.unit.nva.search2.model.OpenSearchQueryBuilder;
import no.unit.nva.search2.sws.OpenSearchSwsClient;
import no.unit.nva.search2.model.OpenSearchSwsResponse;
import no.unit.nva.search2.model.PagedSearchResourceDto;
import nva.commons.apigateway.exceptions.ApiGatewayException;
import nva.commons.core.paths.UriWrapper;
import org.jetbrains.annotations.NotNull;
import org.opensearch.action.search.SearchResponse;

import java.net.URI;
import java.util.Map;
import java.util.stream.Stream;

import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;
import static no.unit.nva.search2.common.ResourceParameterKey.FROM;
import static no.unit.nva.search2.common.ResourceParameterKey.PAGE;
import static no.unit.nva.search2.common.ResourceParameterKey.SEARCH_AFTER;
import static no.unit.nva.search2.common.ResourceParameterKey.SIZE;
import static no.unit.nva.search2.common.ResourceParameterKey.SORT;
import static no.unit.nva.search2.common.ResourceParameterKey.VALID_LUCENE_PARAMETER_KEYS;
import static no.unit.nva.search2.common.ResourceParameterKey.keyFromString;
import static no.unit.nva.search2.constant.Defaults.DEFAULT_OFFSET;
import static no.unit.nva.search2.constant.Defaults.DEFAULT_VALUE_PER_PAGE;
import static no.unit.nva.search2.constant.Defaults.DEFAULT_VALUE_SORT;
import static no.unit.nva.search2.constant.Defaults.DEFAULT_VALUE_SORT_ORDER;

public final class ResourceQuery2 extends OpenSearchQuery<ResourceParameterKey, PagedSearchResourceDto> {

//    @Override
    public PagedSearchResourceDto doSearch(OpenSearchSwsClient queryClient) throws ApiGatewayException {
        return
            Stream.of(queryClient.doSearch(queryClient))
                .map(this::toPagedSearchResponseDto)
            .findFirst().orElseThrow();
    }


    @Override
    public PagedSearchResourceDto doSearch(OpenSearchClient<?, ?> queryClient) throws ApiGatewayException {
        return null;
    }

    private ResourceQuery2() {
        super();
    }


    @NotNull
    private PagedSearchResourceDto toPagedSearchResponseDto(@NotNull SearchResponse response) {

        final var offset = getQueryFrom();
        final var url = gatewayUri.toString().split("\\?")[0];
        final var requestParameter = toGateWayRequestParameter();
        final var id = createUriOffsetRef(url, requestParameter, offset);
        final var hasMoreResults = offset < response.getHits().getTotalHits().value;
        var nextResults
            = hasMoreResults
            ? createUriOffsetRef(url, requestParameter, offset + getQuerySize())
            : null;
        var nextResultsBySortKey
            = hasMoreResults
            ? getNextResultsBySortKey(response, requestParameter, url)
            : null;

        var hasPreviousResults = offset > 0;
        var previousResults
            = hasPreviousResults
            ? createUriOffsetRef(url, requestParameter, offset - getQuerySize())
            : null;

        return PagedSearchResourceDto.Builder.builder()
            .withTotalHits(response.getTotalSize())
            .withHits(response.getSearchHits())
            .withAggregations(response.getAggregationsStructured())
            .withId(id)
            .withNextResults(nextResults)
            .withPreviousResults(previousResults)
            .withNextResultsBySortKey(nextResultsBySortKey)
            .build();
    }


    @SuppressWarnings("PMD.NullAssignment")
    @NotNull
    private PagedSearchResourceDto toPagedSearchResponseDto(@NotNull OpenSearchSwsResponse response) {

        final var offset = getQueryFrom();
        final var url = gatewayUri.toString().split("\\?")[0];
        final var requestParameter = toGateWayRequestParameter();
        final var id = createUriOffsetRef(url, requestParameter, offset);
        final var hasMoreResults = offset < response.getTotalSize();
        var nextResults
            = hasMoreResults
                  ? createUriOffsetRef(url, requestParameter, offset + getQuerySize())
                  : null;
        var nextResultsBySortKey
            = hasMoreResults
                  ? getNextResultsBySortKey(response, requestParameter, url)
                  : null;

        var hasPreviousResults = offset > 0;
        var previousResults
            = hasPreviousResults
                  ? createUriOffsetRef(url, requestParameter, offset - getQuerySize())
                  : null;

        return PagedSearchResourceDto.Builder.builder()
                   .withTotalHits(response.getTotalSize())
                   .withHits(response.getSearchHits())
                   .withAggregations(response.getAggregationsStructured())
                   .withId(id)
                   .withNextResults(nextResults)
                   .withPreviousResults(previousResults)
                   .withNextResultsBySortKey(nextResultsBySortKey)
                   .build();
    }

    public static int compareParameterKey(ResourceParameterKey resourceParameterKey,
                                          ResourceParameterKey resourceParameterKey1) {
        return resourceParameterKey.ordinal() - resourceParameterKey1.ordinal();
    }

    private URI getNextResultsBySortKey(
        @NotNull OpenSearchSwsResponse response, Map<String, String> requestParameter, String url
    ) {
        requestParameter.remove(FROM.key());
        var sortedP = String.join(",", response.getSort().stream().map(Object::toString).toList());
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


    @NotNull
    private Long getQueryFrom() {
        return Stream.of(this.getValue(FROM))
                   .map(Long::parseLong).findFirst()
                   .orElse(0L);
    }


    @NotNull
    private Long getQuerySize() {
        return Long.getLong(getValue(SIZE), Long.parseLong(DEFAULT_VALUE_PER_PAGE));
    }

    public static final class Builder
        extends OpenSearchQueryBuilder<ResourceParameterKey, PagedSearchResourceDto> {

        public static final String ALL = "all";

        private Builder() {
            super(new ResourceQuery2());
        }

        public static Builder queryBuilder() {
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
            var qpKey = keyFromString(key, value);
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
            query.setQueryValue(SORT, mergeParameters(query.getValue(SORT), value));
        }

        @Override
        protected void applyRulesAfterValidation() {
            // convert page to offset if offset is not set
            if (nonNull(query.getValue(PAGE))) {
                if (isNull(query.getValue(FROM))) {
                    var page = Integer.parseInt(query.getValue(PAGE));
                    var perPage = Integer.parseInt(query.getValue(SIZE));
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
            query.setQueryValue(qpKey, mergeParameters(query.getValue(qpKey), validFieldValue));
        }
    }

}