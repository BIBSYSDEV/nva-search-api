package no.unit.nva.search2;


import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;
import static no.unit.nva.search2.ResourceParameter.OFFSET;
import static no.unit.nva.search2.ResourceParameter.PAGE;
import static no.unit.nva.search2.ResourceParameter.PER_PAGE;
import static no.unit.nva.search2.ResourceParameter.keyFromString;
import static no.unit.nva.search2.constant.Defaults.DEFAULT_VALUE_PAGE;
import static no.unit.nva.search2.constant.Defaults.DEFAULT_VALUE_PER_PAGE;
import static no.unit.nva.search2.constant.Defaults.DEFAULT_VALUE_SORT;
import static no.unit.nva.search2.constant.Defaults.DEFAULT_VALUE_SORT_ORDER;

import no.unit.nva.search2.common.OpenSearchQuery;
import no.unit.nva.search2.model.PagedSearchResponseDto;
import no.unit.nva.search2.common.QueryBuilder;
import no.unit.nva.search2.model.OpenSearchSwsResponse;

import java.util.stream.Stream;
import org.jetbrains.annotations.NotNull;

public final class ResourceQuery extends OpenSearchQuery<ResourceParameter,PagedSearchResponseDto> {

    @Override
    public PagedSearchResponseDto doSearch(@NotNull SwsOpenSearchClient queryClient) {
        logger.info("Requesting search from {}", openSearchUri());

        return
            Stream.of(queryClient.doSearch(openSearchUri()))
                .map(this::toPagedSearchResponseDto)
                .findFirst().orElseThrow();
    }

    private ResourceQuery() {
        super();
    }

    @SuppressWarnings("PMD.NullAssignment")
    @NotNull
    private PagedSearchResponseDto toPagedSearchResponseDto(@NotNull OpenSearchSwsResponse response) {

        var offset = getOffset();
        var url = gatewayUri.toString().split("\\?")[0];

        return PagedSearchResponseDto.Builder.builder()
            .withTotalHits(response.getTotalSize())
            .withHits(response.getSearchHits())
            .withSort(response.getSort())
            .withAggregations(response.getAggregationsStructured())
            .withRootUrl(url)
            .withRequestParameters(toGateWayRequestParameter())
            .withOffset(offset)
            .withNextOffset(offset + getPageSize())
            .withPreviousOffset(offset - getPageSize())
            .build();
    }

    @NotNull
    private Long getOffset() {
        return Stream.of(this.getValue(OFFSET))
                   .map(Long::parseLong).findFirst()
                   .orElse(getPage() * getPageSize());
    }

    @NotNull
    private Long getPage() {
        return Long.getLong(getValue(PAGE), 0);
    }

    @NotNull
    private Long getPageSize() {
        return Long.getLong(getValue(PER_PAGE), Long.parseLong(DEFAULT_VALUE_PER_PAGE));
    }

    public static final class Builder extends QueryBuilder<ResourceParameter, PagedSearchResponseDto> {
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
                    case PAGE, OFFSET -> setValue(key.key(), DEFAULT_VALUE_PAGE);
                    case PER_PAGE -> setValue(key.key(), DEFAULT_VALUE_PER_PAGE);
                    case SORT -> setValue(key.key(), DEFAULT_VALUE_SORT);
                    case SORT_ORDER -> setValue(key.key(), DEFAULT_VALUE_SORT_ORDER);
                    default -> {
                    }
                }
            });
        }

        @Override
        protected void setValue(String key, String value) {
            var qpKey = keyFromString(key, value);
            switch (qpKey) {
                case FIELDS,
                         SORT, SORT_ORDER, SEARCH_AFTER,
                         OFFSET, PER_PAGE, PAGE -> query.setQueryValue(qpKey, value);
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

        @Override
        protected void applyRulesAfterValidation() {
            // convert page to offset if offset is not set
            if (isNull(query.getValue(OFFSET)) && nonNull(query.getValue(PAGE))) {
                var page = Integer.parseInt(query.getValue(PAGE));
                var perPage = Integer.parseInt(query.getValue(PER_PAGE));
                query.setQueryValue(OFFSET, String.valueOf(page * perPage));
                query.removeValue(PAGE);
            }
        }
    }
}