package no.unit.nva.search2.model;

import static java.util.Objects.isNull;
import static no.unit.nva.search2.constant.Defaults.PAGINATED_SEARCH_RESULT_CONTEXT;
import static no.unit.nva.search2.model.ResourceParameterKey.FROM;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.JsonNode;
import java.net.URI;
import java.util.List;
import java.util.Map;
import nva.commons.core.paths.UriWrapper;
import org.jetbrains.annotations.Nullable;

public record PagedSearchResourceDto(
    URI id,
    URI nextResults,
    URI previousResults,
    long totalHits,
    List<JsonNode> hits,
    URI nextResultsBySortKey,
    JsonNode aggregations) {

    @JsonProperty("@context")
    public URI context() {
        return PAGINATED_SEARCH_RESULT_CONTEXT;
    }

    private PagedSearchResourceDto(Builder builder) {
        this(builder.id,
             builder.nextResults,
             builder.previousResults,
             builder.totalHits,
             builder.hits,
             builder.nextResultsBySortKey,
             builder.aggregations
        );
    }

    @SuppressWarnings({"PMD.UnusedPrivateField", "PMD.SingularField"})
    public static final class Builder {

        private URI id;
        private URI nextResults;
        private URI previousResults;
        private URI nextResultsBySortKey;
        private long totalHits;
        private List<JsonNode> hits;
        private JsonNode aggregations;

        private Builder() {
        }

        public static Builder builder() {
            return new Builder();
        }

        public Builder withIds(URI gatewayUri, Map<String, String> requestParameter, Integer offset, Integer size) {
            this.id =
                createUriOffsetRef(requestParameter, offset, gatewayUri);
            this.previousResults =
                createUriOffsetRef(requestParameter, offset - size, gatewayUri);

            this.nextResults =
                createNextResults(requestParameter, offset + size, totalHits, gatewayUri);
            return this;
        }

        public Builder withNextResultsBySortKey(URI nextResultsBySortKey) {
            this.nextResultsBySortKey = nextResultsBySortKey;
            return this;
        }

        public Builder withTotalHits(long totalHits) {
            this.totalHits = totalHits;
            return this;
        }

        public Builder withHits(List<JsonNode> hits) {
            this.hits = hits;
            return this;
        }

        public Builder withAggregations(JsonNode aggregations) {
            this.aggregations = aggregations;
            return this;
        }

        @Nullable
        private URI createNextResults(Map<String, String> requestParameter, Integer offset, Long totalSize,
                                      URI gatewayUri) {
            return offset < totalSize
                       ? createUriOffsetRef(requestParameter, offset, gatewayUri)
                       : null;
        }

        private URI createUriOffsetRef(Map<String, String> params, Integer offset, URI gatewayUri) {
            if (offset < 0) {
                return null;
            }

            params.put(FROM.key(), String.valueOf(offset));
            return UriWrapper.fromUri(gatewayUri)
                       .addQueryParameters(params)
                       .getUri();
        }

        public PagedSearchResourceDto build() {
            if (isNull(this.nextResults)) {
                this.nextResultsBySortKey = null;
            }

            return new PagedSearchResourceDto(this);
        }
    }
}
