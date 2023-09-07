package no.unit.nva.search2;


import static no.unit.nva.search2.ResourceParameter.PAGE;
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

public class ResourceQuery extends OpenSearchQuery<ResourceParameter,PagedSearchResponseDto> {

    public static ResourceQueryBuilder builder() {
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

    private PagedSearchResponseDto toPagedSearchResponseDto(SwsOpenSearchResponse response) {
        var gwparams = toGateWayRequestParameter();
        var page = Integer.getInteger(gwparams.get(PAGE.key()), 0);
        var source = gatewayUri.toString().split("\\?")[0];
        return new PagedSearchResponseDto(
            DEFAULT_SEARCH_CONTEXT,
            uriPage(source, gwparams, --page),
            uriPage(source, gwparams, ++page),
            uriPage(source, gwparams, ++page),
            response.took(),
            response.getTotalSize(),
            response.getSearchHits(),
            response.getAggregationsStructured()
        );
    }

    private URI uriPage(String source, Map<String, String> params, Integer page) {
        if (page < 0) {
            return null;
        }
        params.put(PAGE.key(), String.valueOf(page));
        return UriWrapper.fromUri(source)
                   .addQueryParameters(params)
                   .getUri();
    }

    public static class ResourceQueryBuilder extends QueryBuilder<ResourceParameter, PagedSearchResponseDto> {

        public ResourceQueryBuilder() {
            super(new ResourceQuery());
        }

        @Override
        protected void assignDefaultValues() {
            requiredMissing().forEach(key -> {
                switch (key) {
                    case PAGE -> setValue(key.key(), DEFAULT_VALUE_PAGE);
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
                         SORT, SORT_ORDER,
                         PER_PAGE, PAGE -> query.setQValue(qpKey, value);
                case CATEGORY, CONTRIBUTOR,
                         CREATED_BEFORE, CREATED_SINCE,
                         DOI, FUNDING, FUNDING_SOURCE, ID,
                         INSTITUTION, ISSN,
                         MODIFIED_BEFORE, MODIFIED_SINCE,
                         PROJECT_CODE, PUBLISHED_BEFORE,
                         PUBLISHED_SINCE, SEARCH_ALL, TITLE,
                         UNIT, USER, YEAR_REPORTED -> query.setValue(qpKey, value);
                case LANG -> {
                    // ignore and continue
                }
                default -> invalidKeys.add(key);
            }
        }
    }
}