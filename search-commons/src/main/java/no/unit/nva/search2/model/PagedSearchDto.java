package no.unit.nva.search2.model;

import static java.util.Objects.isNull;
import static no.unit.nva.search2.constant.ApplicationConstants.objectMapperWithEmpty;
import static no.unit.nva.search2.constant.Defaults.PAGINATED_SEARCH_RESULT_CONTEXT;
import static no.unit.nva.search2.model.parameterkeys.ResourceParameter.FROM;
import static nva.commons.core.attempt.Try.attempt;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.JsonNode;
import java.net.URI;
import java.util.List;
import java.util.Map;

import no.unit.nva.commons.json.JsonSerializable;
import nva.commons.core.paths.UriWrapper;
import org.jetbrains.annotations.Nullable;

public record PagedSearchDto<T>(
    URI id,
    int totalHits,
    List<T> hits,
    URI nextResults,
    URI nextSearchAfterResults,
    URI previousResults,
    JsonNode aggregations) implements JsonSerializable {

    private PagedSearchDto(Builder builder) {
        this(builder.id,
             builder.totalHits,
            (List<T>) builder.hits,
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


    @SuppressWarnings({"PMD.UnusedPrivateField", "PMD.SingularField"})
    public static final class Builder<T> {

        private URI id;
        private URI nextResults;
        private URI previousResults;
        private URI nextResultsBySortKey;
        private int totalHits;
        private List<T> hits;
        private JsonNode aggregations;

        private Builder() {
        }

        public <T> Builder<T> builder() {
            return new Builder<>();
        }

        @SuppressWarnings("PMD.NullAssignment")
        public PagedSearchDto<T> build() {
            if (isNull(this.nextResults)) {
                this.nextResultsBySortKey = null;       // null values are not serialized
            }

            return new PagedSearchDto<>(this);
        }


        public Builder<T> withIds(URI gatewayUri, Map<String, String> requestParameter, Integer offset, Integer size) {
            this.id =
                createUriOffsetRef(requestParameter, offset, gatewayUri);
            this.previousResults =
                createUriOffsetRef(requestParameter, offset - size, gatewayUri);

            this.nextResults =
                createNextResults(requestParameter, offset + size, totalHits, gatewayUri);
            return this;
        }

        public Builder<T> withNextResultsBySortKey(URI nextResultsBySortKey) {
            this.nextResultsBySortKey = nextResultsBySortKey;
            return this;
        }

        public Builder<T> withTotalHits(int totalHits) {
            this.totalHits = totalHits;
            return this;
        }

        public Builder<T> withHits(List<T> hits) {
            this.hits = hits;
            return this;
        }

        public Builder<T> withAggregations(JsonNode aggregations) {
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

    }
}
