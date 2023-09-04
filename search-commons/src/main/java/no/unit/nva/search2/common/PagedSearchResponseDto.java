package no.unit.nva.search2.common;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.JsonNode;
import nva.commons.core.paths.UriWrapper;

import java.net.URI;
import java.util.List;

import static no.unit.nva.search2.ResourceParameter.PAGE;
import static no.unit.nva.search2.common.OpenSearchQuery.queryToMap;
import static no.unit.nva.search2.constants.Defaults.PAGINATED_SEARCH_RESULT_CONTEXT;

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

    public static URI nextResults(URI id) {
        var params = queryToMap(id);
        if (!params.containsKey(PAGE.getKey())) {
            return null;
        }
        var page = Integer.parseInt(params.get(PAGE.getKey()));
        params.put(PAGE.getKey(),  String.valueOf(++page));
        return UriWrapper.fromUri(id)
            .addQueryParameters(params)
            .getUri();
    }

    public static URI previousResults(URI id) {
        var params = queryToMap(id);
        if (!params.containsKey(PAGE.getKey())) {
            return null;
        }
        var page = Integer.parseInt(params.get(PAGE.getKey()));
        if (page <= 0) {
            return null;
        }
        params.put(PAGE.getKey(),String.valueOf(--page));
        return UriWrapper.fromUri(id)
            .addQueryParameters(params)
            .getUri();
    }

}
