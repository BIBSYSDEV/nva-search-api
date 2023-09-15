package no.unit.nva.search2;


import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;
import static no.unit.nva.search2.ResourceParameterKey.OFFSET;
import static no.unit.nva.search2.ResourceParameterKey.PAGE;
import static no.unit.nva.search2.ResourceParameterKey.RESULTS;
import static no.unit.nva.search2.ResourceParameterKey.SORT;
import static no.unit.nva.search2.ResourceParameterKey.keyFromString;
import static no.unit.nva.search2.constant.Defaults.DEFAULT_OFFSET;
import static no.unit.nva.search2.constant.Defaults.DEFAULT_VALUE_PER_PAGE;
import static no.unit.nva.search2.constant.Defaults.DEFAULT_VALUE_SORT;
import static no.unit.nva.search2.constant.Defaults.DEFAULT_VALUE_SORT_ORDER;
import static org.apache.http.entity.ContentType.APPLICATION_JSON;

import no.unit.nva.search2.common.OpenSearchQuery;
import no.unit.nva.search2.common.OpenSearchSwsClient;
import no.unit.nva.search2.model.PagedSearchResourceDto;
import no.unit.nva.search2.common.OpenSearchQueryBuilder;
import no.unit.nva.search2.model.OpenSearchSwsResponse;

import java.util.stream.Stream;

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

        var offset = getQueryOffset();
        var url = gatewayUri.toString().split("\\?")[0];

        return PagedSearchResourceDto.Builder.builder()
            .withTotalHits(response.getTotalSize())
            .withHits(response.getSearchHits())
            .withSort(response.getSort())
            .withAggregations(response.getAggregationsStructured())
            .withRootUrl(url)
            .withRequestParameters(toGateWayRequestParameter())
            .withOffset(offset)
            .withOffsetNext(offset + getQueryPageSize())
            .withOffsetPrevious(offset - getQueryPageSize())
            .build();
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
        return Long.getLong(getValue(RESULTS), Long.parseLong(DEFAULT_VALUE_PER_PAGE));
    }

    public static final class Builder
        extends OpenSearchQueryBuilder<ResourceParameterKey, PagedSearchResourceDto> {
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
                    case RESULTS -> setValue(key.key(), DEFAULT_VALUE_PER_PAGE);
                    case SORT -> setValue(key.key(), DEFAULT_VALUE_SORT+ ":" + DEFAULT_VALUE_SORT_ORDER);
                    default -> {
                    }
                }
            });
        }

        @Override
        protected void setValue(String key, String value) {
            var qpKey = keyFromString(key, value);
            switch (qpKey) {
                case SEARCH_AFTER, OFFSET,
                    RESULTS, PAGE -> query.setQueryValue(qpKey, value);
                case SORT -> {
                    var validFieldValue =  decodeUTF(value).replaceAll(" (asc|desc)", ":$1");
                    query.setQueryValue(qpKey, mergeParameters(query.getValue(qpKey),validFieldValue));
                }
                case SORT_ORDER -> query.setQueryValue(SORT, mergeParameters(query.getValue(SORT),value));
                case CATEGORY, CONTRIBUTOR,
                         CREATED_BEFORE, CREATED_SINCE,
                         DOI, FUNDING, FUNDING_SOURCE, ID,
                         INSTITUTION, ISSN,
                         MODIFIED_BEFORE, MODIFIED_SINCE,
                         PROJECT_CODE, PUBLISHED_BEFORE,
                         PUBLISHED_SINCE, SEARCH_ALL, TITLE,
                         UNIT, USER, YEAR_REPORTED -> query.setLucineValue(qpKey, value);
                case FIELDS, LANG -> {
                    // ignore and continue
                    // TODO -> fields, when we have defined a simple search response, we can implement this
                }
                default -> invalidKeys.add(key);
            }
        }

        @Override
        protected void applyRulesAfterValidation() {
            // convert page to offset if offset is not set
            if (isNull(query.getValue(OFFSET)) && nonNull(query.getValue(PAGE))) {
                var page = Integer.parseInt(query.getValue(PAGE));
                var perPage = Integer.parseInt(query.getValue(RESULTS));
                query.setQueryValue(OFFSET, String.valueOf(page * perPage));
                query.removeValue(PAGE);
            }
        }
    }

}