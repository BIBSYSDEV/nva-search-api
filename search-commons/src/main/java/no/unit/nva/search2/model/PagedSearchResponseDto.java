package no.unit.nva.search2.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.JsonNode;
import nva.commons.core.paths.UriWrapper;

import java.net.URI;
import java.util.List;
import java.util.Map;

import static java.util.Objects.nonNull;
import static no.unit.nva.search2.ResourceParameter.OFFSET;
import static no.unit.nva.search2.constant.Defaults.PAGINATED_SEARCH_RESULT_CONTEXT;

public record PagedSearchResponseDto (
    URI id,
    URI nextResults,
    URI previousResults,
    long totalHits,
    List<JsonNode> hits,
    List<Long> lastSortKeyIndex,
    JsonNode aggregations) {

    @JsonProperty("@context")
    public URI context() {
        return PAGINATED_SEARCH_RESULT_CONTEXT;
    }

    private PagedSearchResponseDto(Builder builder) {
        this(builder.id,
            builder.nextResults,
            builder.previousResults,
            builder.totalHits,
            builder.hits,
            builder.lastSortKeyIndex,
            builder.aggregations
        );
    }

    @SuppressWarnings({"PMD.UnusedPrivateField", "PMD.SingularField"})
    public static final class Builder {
        private URI id;
        private URI nextResults;
        private URI previousResults;
        private long totalHits;
        private List<JsonNode> hits;
        private List<Long> lastSortKeyIndex;
        private JsonNode aggregations;
        private String rootUrl;
        private Long offset;
        private Long nextOffset;
        private Long previousOffset;
        private Map<String, String> parameters;

        private Builder() {
        }

        public static Builder builder() {
            return new Builder();
        }

        public Builder withRequestParameters(Map<String, String> params) {
            this.parameters = params;
            return this;
        }

        public Builder withRootUrl(String rootUrl) {
            this.rootUrl = rootUrl;
            return this;
        }

        public Builder withNextOffset(Long offset) {
            this.nextOffset = offset;
            return this;
        }

        public Builder withPreviousOffset(Long offset) {
            this.previousOffset = offset;
            return this;
        }

        public Builder withOffset(Long offset) {
            this.offset = offset;
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

        public Builder withSort(List<Long> sort) {
            this.lastSortKeyIndex = sort;
            return this;
        }

        public Builder withAggregations(JsonNode aggregations) {
            this.aggregations = aggregations;
            return this;
        }


        private URI createUriOffsetRef(Long offset) {
            if (offset < 0) {
                return null;
            }
            parameters.put(OFFSET.key(), offset.toString());
            return UriWrapper.fromUri(rootUrl)
                .addQueryParameters(parameters)
                .getUri();
        }

        public PagedSearchResponseDto build() {
            if (nonNull(totalHits)) {
                if (nonNull(offset)) {
                    this.id = createUriOffsetRef(offset);
                }
                if (nextOffset < totalHits) {
                    this.nextResults = createUriOffsetRef(nextOffset);
                }
                if (previousOffset >= 0) {
                    this.previousResults = createUriOffsetRef(previousOffset);
                }
            }
            return new PagedSearchResponseDto(this);
        }
    }
}
