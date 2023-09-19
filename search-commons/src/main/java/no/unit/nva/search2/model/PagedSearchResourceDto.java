package no.unit.nva.search2.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.JsonNode;

import java.net.URI;
import java.util.List;
import static no.unit.nva.search2.constant.Defaults.PAGINATED_SEARCH_RESULT_CONTEXT;

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

        public Builder withId(URI id) {
            this.id = id;
            return this;
        }

        public Builder withNextResults(URI nextResults) {
            this.nextResults = nextResults;
            return this;
        }

        public Builder withPreviousResults(URI previousResults) {
            this.previousResults = previousResults;
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

        public PagedSearchResourceDto build() {
            return new PagedSearchResourceDto(this);
        }
    }
}
