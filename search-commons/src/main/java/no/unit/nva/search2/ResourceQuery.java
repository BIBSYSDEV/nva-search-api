package no.unit.nva.search2;


import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;
import static no.unit.nva.search2.ResourceParameterKey.OFFSET;
import static no.unit.nva.search2.ResourceParameterKey.PAGE;
import static no.unit.nva.search2.ResourceParameterKey.PER_PAGE;
import static no.unit.nva.search2.ResourceParameterKey.SEARCH_AFTER;
import static no.unit.nva.search2.ResourceParameterKey.SORT;
import static no.unit.nva.search2.ResourceParameterKey.VALID_LUCENE_PARAMETER_KEYS;
import static no.unit.nva.search2.ResourceParameterKey.keyFromString;
import static no.unit.nva.search2.constant.Defaults.DEFAULT_OFFSET;
import static no.unit.nva.search2.constant.Defaults.DEFAULT_VALUE_PER_PAGE;
import static no.unit.nva.search2.constant.Defaults.DEFAULT_VALUE_SORT;
import static no.unit.nva.search2.constant.Defaults.DEFAULT_VALUE_SORT_ORDER;
import static org.apache.http.entity.ContentType.APPLICATION_JSON;

import java.net.URI;
import java.util.Map;
import no.unit.nva.search2.common.OpenSearchQuery;
import no.unit.nva.search2.common.OpenSearchSwsClient;
import no.unit.nva.search2.model.PagedSearchResourceDto;
import no.unit.nva.search2.common.OpenSearchQueryBuilder;
import no.unit.nva.search2.model.OpenSearchSwsResponse;

import java.util.stream.Stream;

import nva.commons.core.paths.UriWrapper;
import org.jetbrains.annotations.NotNull;

public final class ResourceQuery extends OpenSearchQuery<ResourceParameterKey, PagedSearchResourceDto> {

    @Override
    public PagedSearchResourceDto doSearch(@NotNull OpenSearchSwsClient queryClient) {
        return
            Stream.of(queryClient.doSearch(openSearchUri(),APPLICATION_JSON.toString()))
                .map(this::toPagedSearchResponseDto)
                .findFirst().orElseThrow();
    }

    private ResourceQuery() {
        super();
    }

    @SuppressWarnings("PMD.NullAssignment")
    @NotNull
    private PagedSearchResourceDto toPagedSearchResponseDto(@NotNull OpenSearchSwsResponse response) {

        final var offset = getQueryOffset();
        final var url = gatewayUri.toString().split("\\?")[0];
        final var requestParameter = toGateWayRequestParameter();
        final var id = createUriOffsetRef(url, requestParameter, offset);
        final var hasMoreResults = offset < response.getTotalSize();
        var nextResults
            = hasMoreResults
                  ? createUriOffsetRef(url, requestParameter, offset + getQueryPageSize())
                  : null;
        var nextResultsBySortKey
            = hasMoreResults
                  ? getNextResultsBySortKey(response, requestParameter, url)
                  : null;

        var hasPreviousResults = offset > 0;
        var previousResults
            = hasPreviousResults
                  ? createUriOffsetRef(url, requestParameter, offset - getQueryPageSize())
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
        requestParameter.remove(OFFSET.key());
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
        params.put(OFFSET.key(), String.valueOf(offset));
        return UriWrapper.fromUri(source)
                   .addQueryParameters(params)
                   .getUri();
    }


    @NotNull
    private Long getQueryOffset() {
        return Stream.of(this.getValue(OFFSET))
                   .map(Long::parseLong).findFirst()
                   .orElse(getQueryPage() * getQueryPageSize());
    }

    @NotNull
    private Long getQueryPage() {
        return Long.getLong(getValue(PAGE), 0);
    }

    @NotNull
    private Long getQueryPageSize() {
        return Long.getLong(getValue(PER_PAGE), Long.parseLong(DEFAULT_VALUE_PER_PAGE));
    }

    public static final class Builder
        extends OpenSearchQueryBuilder<ResourceParameterKey, PagedSearchResourceDto> {

        public static final String ALL = "all";

        private Builder() {
            super(new ResourceQuery());
        }

        public static Builder queryBuilder() {
            return new Builder();
        }

        @Override
        protected void assignDefaultValues() {
            requiredMissing().forEach(key -> {
                switch (key) {
                    case PAGE, OFFSET -> setValue(key.key(), DEFAULT_OFFSET);
                    case PER_PAGE -> setValue(key.key(), DEFAULT_VALUE_PER_PAGE);
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
                         OFFSET,
                         PER_PAGE,
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
                if (isNull(query.getValue(OFFSET))) {
                    var page = Integer.parseInt(query.getValue(PAGE));
                    var perPage = Integer.parseInt(query.getValue(PER_PAGE));
                    query.setQueryValue(OFFSET, String.valueOf(page * perPage));
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