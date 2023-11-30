package no.unit.nva.search2.dto;

import static java.util.Objects.isNull;
import com.fasterxml.jackson.databind.JsonNode;
import java.net.URI;
import java.util.List;
import java.util.Map;
import nva.commons.core.paths.UriWrapper;
import org.jetbrains.annotations.Nullable;

public class PagedSearchBuilder {

    private URI id;
    private int totalHits;
    private List<JsonNode> hits;
    private URI nextResults;
    private URI nextSearchAfterResults;
    private URI previousResults;
    private Map<String, List<Facet>> aggregations;

    @SuppressWarnings("PMD.NullAssignment")
    public PagedSearch build() {
        if (isNull(this.nextResults)) {
            this.nextSearchAfterResults = null;       // null values are not serialized
        }
        return new PagedSearch(
            id,
            totalHits,
            hits,
            nextResults,
            nextSearchAfterResults,
            previousResults,
            aggregations
        );
    }

    public PagedSearchBuilder withIds(URI gatewayUri, Map<String, String> requestParameter, Integer offset,
                                      Integer size) {
        this.id = createUriOffsetRef(requestParameter, offset, gatewayUri);
        this.previousResults = createUriOffsetRef(requestParameter, offset - size, gatewayUri);
        this.nextResults = createNextResults(requestParameter, offset + size, totalHits, gatewayUri);
        return this;
    }

    public PagedSearchBuilder withNextResultsBySortKey(URI nextSearchAfterResults) {
        this.nextSearchAfterResults = nextSearchAfterResults;
        return this;
    }

    public PagedSearchBuilder withTotalHits(int totalHits) {
        this.totalHits = totalHits;
        return this;
    }

    public PagedSearchBuilder withHits(List<JsonNode> hits) {
        this.hits = hits;
        return this;
    }

    public PagedSearchBuilder withAggregations(JsonNode aggregations) {

        if (isNull(aggregations)) {
            return this;
        }
        this.aggregations = FacetsBuilder.build(aggregations, this.id);
        return this;
    }

    @Nullable
    private URI createNextResults(Map<String, String> requestParameter, Integer offset, Integer totalSize,
                                  URI gatewayUri) {
        return offset < totalSize
            ? createUriOffsetRef(requestParameter, offset, gatewayUri)
            : null;
    }

    private URI createUriOffsetRef(Map<String, String> params, Integer offset, URI gatewayUri) {
        if (offset < 0) {
            return null;
        }

        params.put("FROM", String.valueOf(offset));
        return UriWrapper.fromUri(gatewayUri)
            .addQueryParameters(params)
            .getUri();
    }
}