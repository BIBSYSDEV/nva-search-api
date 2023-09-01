package no.unit.nva.search2.common;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.JsonNode;
import nva.commons.core.paths.UriWrapper;

import java.net.URI;
import java.util.List;

import static no.unit.nva.search2.ResourceParameter.PAGE;
import static no.unit.nva.search2.common.OpenSearchQuery.queryToMap;

public record PagedSearchResponseDto (
    @JsonProperty("@context")URI context,
    URI id,
    URI nextResults,
    URI previousResults,
    long processingTime,
    long size,
    List<JsonNode> hits,
    JsonNode aggregations)  {

    @Override
    public URI context() {
        return URI.create(PAGINATED_SEARCH_RESULT_CONTEXT);
    }

    @Override
    public URI nextResults() {
        var params = queryToMap(id());
        var page = Integer.parseInt(params.get(PAGE.getKey()));
        params.put(PAGE.getKey(),  String.valueOf(++page));
        return UriWrapper.fromUri(id())
            .addQueryParameters(params)
            .getUri();
    }

    @Override
    public URI previousResults() {
        var params = queryToMap(id());
        var page = Integer.parseInt(params.get(PAGE.getKey()));
        if (page <= 0) return null;
        params.put(PAGE.getKey(),String.valueOf(--page));
        return UriWrapper.fromUri(id())
            .addQueryParameters(params)
            .getUri();
    }


    private static final String PAGINATED_SEARCH_RESULT_CONTEXT
        = "https://bibsysdev.github.io/src/search/paginated-search-result.json";
}
