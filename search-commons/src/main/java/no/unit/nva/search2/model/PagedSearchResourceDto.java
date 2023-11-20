package no.unit.nva.search2.model;

import static java.util.Objects.isNull;
import static no.unit.nva.search2.constant.ApplicationConstants.objectMapperWithEmpty;
import static no.unit.nva.search2.constant.Defaults.PAGINATED_SEARCH_RESULT_CONTEXT;
import static no.unit.nva.search2.model.ParameterKeyResources.FROM;
import static nva.commons.core.attempt.Try.attempt;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.JsonNode;
import java.net.URI;
import java.util.List;
import java.util.Map;
import nva.commons.core.paths.UriWrapper;
import org.jetbrains.annotations.Nullable;

public record PagedSearchResourceDto(
    URI id,
    int totalHits,
    List<JsonNode> hits,
    URI nextResults,
    URI nextSearchAfterResults,
    URI previousResults,
    JsonNode aggregations) {

    private PagedSearchResourceDto(Builder builder) {
        this(builder.id,
             builder.totalHits,
             builder.hits,
             builder.nextResults,
             builder.nextResultsBySortKey,
             builder.previousResults,
             builder.aggregations
        );
    }

    @JsonProperty("@context")
    public URI context() {
        return PAGINATED_SEARCH_RESULT_CONTEXT;
    }

    public String toJsonString() {
        return attempt(() -> objectMapperWithEmpty.writeValueAsString(this))
                   .orElseThrow();
    }

    @SuppressWarnings({"PMD.UnusedPrivateField", "PMD.SingularField"})
    public static final class Builder {

        private URI id;
        private URI nextResults;
        private URI previousResults;
        private URI nextResultsBySortKey;
        private int totalHits;
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

        public Builder withTotalHits(int totalHits) {
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

        @SuppressWarnings("PMD.NullAssignment")
        public PagedSearchResourceDto build() {
            if (isNull(this.nextResults)) {
                this.nextResultsBySortKey = null;       // null values are not serialized
            }

            return new PagedSearchResourceDto(this);
        }
    }
}
