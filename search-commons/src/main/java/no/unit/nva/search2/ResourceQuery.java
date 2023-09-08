package no.unit.nva.search2;


import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;
import static no.unit.nva.search2.ResourceParameter.OFFSET;
import static no.unit.nva.search2.ResourceParameter.PAGE;
import static no.unit.nva.search2.ResourceParameter.PER_PAGE;
import static no.unit.nva.search2.ResourceParameter.keyFromString;
import static no.unit.nva.search2.constant.Defaults.DEFAULT_SEARCH_CONTEXT;
import static no.unit.nva.search2.constant.Defaults.DEFAULT_VALUE_PAGE;
import static no.unit.nva.search2.constant.Defaults.DEFAULT_VALUE_PER_PAGE;
import static no.unit.nva.search2.constant.Defaults.DEFAULT_VALUE_SORT;
import static no.unit.nva.search2.constant.Defaults.DEFAULT_VALUE_SORT_ORDER;

import java.util.Map;
import no.unit.nva.search2.common.OpenSearchQuery;
import no.unit.nva.search2.model.PagedSearchResponseDto;
import no.unit.nva.search2.common.QueryBuilder;
import no.unit.nva.search2.model.SwsOpenSearchResponse;
import nva.commons.core.paths.UriWrapper;

import java.net.URI;
import java.util.stream.Stream;
import org.jetbrains.annotations.NotNull;

public class ResourceQuery extends OpenSearchQuery<ResourceParameter,PagedSearchResponseDto> {

    public ResourceQueryBuilder builder() {
        return new ResourceQueryBuilder();
    }

    @Override
    public PagedSearchResponseDto doSearch(SwsOpenSearchClient queryClient) {
        logger.info("Requesting search from {}", openSearchUri());

        return
            Stream.of(queryClient.doSearch(openSearchUri()).body())
                .map(this::toPagedSearchResponseDto)
                .findFirst().orElseThrow();
    }

    @SuppressWarnings("PMD.NullAssignment")
    private PagedSearchResponseDto toPagedSearchResponseDto(SwsOpenSearchResponse response) {
        var requestParameter = toGateWayRequestParameter();

        var offset = getOffset();
        var hasMoreResults =  offset < response.getTotalSize();
        var url = gatewayUri.toString().split("\\?")[0];
        return new PagedSearchResponseDto(
            DEFAULT_SEARCH_CONTEXT,
            createUriOffsetRef(url, requestParameter, offset),
            hasMoreResults ? createUriOffsetRef(url, requestParameter, offset + getPageSize()) : null,
            createUriOffsetRef(url, requestParameter, offset - getPageSize()),
            response.getTotalSize(),
            response.getSearchHits(),
            response.getSort(),
            response.getAggregationsStructured()
        );
    }

    @NotNull
    private Integer getOffset() {
        return Stream.of(this.getValue(OFFSET))
                   .map(Integer::parseInt).findFirst()
                   .orElse(getPage() * getPageSize());
    }

    @NotNull
    private Integer getPage() {
        return  Integer.getInteger(getValue(PAGE), 0);
    }

    @NotNull
    private Integer getPageSize() {
        return Integer.getInteger(getValue(PER_PAGE), Integer.parseInt(DEFAULT_VALUE_PER_PAGE));
    }

    private URI createUriOffsetRef(String source, Map<String, String> params, Integer offset) {
        if (offset < 0) {
            return null;
        }
        params.put(OFFSET.key(), String.valueOf(offset));
        return UriWrapper.fromUri(source)
                   .addQueryParameters(params)
                   .getUri();
    }

    public class ResourceQueryBuilder extends QueryBuilder<ResourceParameter, PagedSearchResponseDto> {

        public ResourceQueryBuilder() {
            super(new ResourceQuery());
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
        protected void applyRules() {
            // convert page to offset if offset is not set
            if (isNull(query.getValue(OFFSET)) && nonNull(query.getValue(PAGE))) {
                var page = Integer.parseInt(query.getValue(PAGE));
                var perPage = Integer.parseInt(query.getValue(PER_PAGE));
                query.setQueryValue(OFFSET, String.valueOf(page * perPage));
            }
            query.removeValue(PAGE);
        }
    }
}