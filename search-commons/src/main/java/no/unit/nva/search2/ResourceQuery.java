package no.unit.nva.search2;


import static no.unit.nva.search2.ResourceParameter.PAGE;
import static no.unit.nva.search2.ResourceParameter.keyFromString;
import static no.unit.nva.search2.constant.Defaults.*;

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
        return
            Stream.of(queryClient.doSearch(openSearchUri()).body())
                .map(this::toPagedSearchResponseDto)
                .findFirst().orElseThrow();
    }

    private PagedSearchResponseDto toPagedSearchResponseDto(SwsOpenSearchResponse response) {
        return new PagedSearchResponseDto(
            DEFAULT_SEARCH_CONTEXT,
            gatewayUri,
            nextResults(gatewayUri),
            previousResults(gatewayUri),
            response.took(),
            response.getSize(),
            response.getHits(),
            response.getAggregations());
    }

    private URI nextResults(URI id) {
        var params = queryToMap(id);
        if (!params.containsKey(PAGE.key())) {
            return null;
        }
        var page = Integer.parseInt(params.get(PAGE.key()));
        params.put(PAGE.key(), String.valueOf(++page));
        return UriWrapper.fromUri(id)
            .addQueryParameters(params)
            .getUri();
    }

    private URI previousResults(URI id) {
        var params = queryToMap(id);
        if (!params.containsKey(PAGE.key())) {
            return null;
        }
        var page = Integer.parseInt(params.get(PAGE.key()));
        if (page <= 0) {
            return null;
        }
        params.put(PAGE.key(), String.valueOf(--page));
        return UriWrapper.fromUri(id)
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
                    default -> { }
                }
            });
        }

        @Override
        protected void setValue(String key, String value) {
            var qpKey = keyFromString(key,value);
            switch (qpKey) {
                case FIELDS,
                         SORT, SORT_ORDER,
                         PER_PAGE, PAGE -> query.setQValue(qpKey, value);
                case CATEGORY,CONTRIBUTOR,
                         CREATED_BEFORE,CREATED_SINCE,
                         DOI,FUNDING,FUNDING_SOURCE,ID,
                         INSTITUTION,ISSN,
                         MODIFIED_BEFORE,MODIFIED_SINCE,
                         PROJECT_CODE,PUBLISHED_BEFORE,
                         PUBLISHED_SINCE, SEARCH_ALL, TITLE,
                         UNIT,USER, YEAR_REPORTED -> query.setValue(qpKey, value);
                case LANG -> {
                    // ignore and continue
                }
                default -> invalidKeys.add(key);
            }
        }
    }

}