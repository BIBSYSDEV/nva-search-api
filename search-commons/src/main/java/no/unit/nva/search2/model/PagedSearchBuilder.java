package no.unit.nva.search2.model;

import static java.util.Objects.isNull;
import static no.unit.nva.search2.model.parameterkeys.ResourceParameter.FROM;
import static nva.commons.core.attempt.Try.attempt;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import java.net.URI;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import no.unit.nva.commons.json.JsonUtils;
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
    public PagedSearchDto build() {
        if (isNull(this.nextResults)) {
            this.nextSearchAfterResults = null;       // null values are not serialized
        }
        return new PagedSearchDto(id, totalHits, hits, nextResults, nextSearchAfterResults, previousResults,
                                  aggregations);
    }

    public PagedSearchBuilder withIds(URI gatewayUri, Map<String, String> requestParameter, Integer offset,
                                      Integer size) {
        this.id =
            createUriOffsetRef(requestParameter, offset, gatewayUri);
        this.previousResults =
            createUriOffsetRef(requestParameter, offset - size, gatewayUri);

        this.nextResults =
            createNextResults(requestParameter, offset + size, totalHits, gatewayUri);
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
        var typeReference = new TypeReference<Map<String, List<Facet>>>() {
        };
        var mappings = attempt(() -> JsonUtils.dtoObjectMapper.readValue(aggregations.toPrettyString(), typeReference))
            .orElseThrow();
        this.aggregations = mappings.entrySet().stream().map(entry -> {
            final var uriwrap = UriWrapper.fromUri(this.id);

            var list = entry.getValue().stream()
                .map(facet -> new Facet(uriwrap.addQueryParameter(entry.getKey(), facet.key()).getUri(), facet.key(),
                                        facet.count(), facet.labels())).toList();
            return Map.entry(entry.getKey(), list);
        }).collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
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

        params.put(FROM.fieldName(), String.valueOf(offset));
        return UriWrapper.fromUri(gatewayUri)
            .addQueryParameters(params)
            .getUri();
    }
}