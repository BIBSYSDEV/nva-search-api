package no.unit.nva.search.models;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.util.Map;
import java.util.Optional;
import nva.commons.core.JacocoGenerated;
import nva.commons.core.paths.UriWrapper;
import org.opensearch.action.search.SearchResponse;
import org.opensearch.search.SearchHit;

import java.net.URI;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import static java.util.Objects.nonNull;
import static no.unit.nva.search.constants.ApplicationConstants.objectMapperWithEmpty;
import static nva.commons.core.attempt.Try.attempt;

public class SearchResponseDto {

    public static final URI DEFAULT_SEARCH_CONTEXT = URI.create("https://api.nva.unit.no/resources/search");
    public static final String QUERY_PARAMETER = "query";
    public static final String WORD_ENDING_WITH_HASHTAG_REGEX = "[A-za-z0-9]*#";

    private static final Map<String, String> AGGREGATION_FIELDS_TO_CHANGE = Map.of(
        "doc_count_error_upper_bound", "docCountErrorUpperBound",
        "sum_other_doc_count", "sumOtherDocCount",
        "doc_count", "docCount");

    @JsonProperty("@context")
    private URI context;
    @JsonProperty("id")
    private URI id;
    @JsonProperty("processingTime")
    private long processingTime;
    @JsonProperty("size")
    private long size;
    private List<JsonNode> hits;
    @JsonProperty("aggregations")
    private JsonNode aggregations;

    public SearchResponseDto() {
    }

    public static SearchResponseDto fromSearchResponse(SearchResponse searchResponse, URI id) {
        List<JsonNode> sourcesList = extractSourcesList(searchResponse);
        long total = searchResponse.getHits().getTotalHits().value;
        long took = searchResponse.getTook().duration();
        var aggregations = extractAggregations(searchResponse);

        return SearchResponseDto.builder()
            .withContext(DEFAULT_SEARCH_CONTEXT)
            .withId(id)
            .withHits(sourcesList)
            .withSize(total)
            .withProcessingTime(took)
            .withAggregations(aggregations)
            .build();
    }

    public static URI createIdWithQuery(URI requestUri, String searchTerm) {
        UriWrapper wrapper = UriWrapper.fromUri(requestUri);
        if (nonNull(searchTerm)) {
            wrapper = wrapper.addQueryParameter(QUERY_PARAMETER, searchTerm);
        }
        return wrapper.getUri();
    }

    public static Builder builder() {
        return new Builder();
    }

    @JacocoGenerated
    public URI getContext() {
        return context;
    }

    public void setContext(URI context) {
        this.context = context;
    }

    @JacocoGenerated
    public URI getId() {
        return id;
    }

    public void setId(URI id) {
        this.id = id;
    }

    @JacocoGenerated
    public long getProcessingTime() {
        return processingTime;
    }

    public void setProcessingTime(long processingTime) {
        this.processingTime = processingTime;
    }

    @JacocoGenerated
    public long getSize() {
        return size;
    }

    public void setSize(long size) {
        this.size = size;
    }

    @JacocoGenerated
    public List<JsonNode> getHits() {
        return hits;
    }

    public void setHits(List<JsonNode> hits) {
        this.hits = hits;
    }

    public void setAggregations(JsonNode aggregations) {
        this.aggregations = aggregations;
    }

    @JsonProperty("total")
    @Deprecated(forRemoval = true)
    @JacocoGenerated
    public Long getTotal() {
        return getSize();
    }

    @JsonProperty("total")
    @Deprecated(forRemoval = true)
    @JacocoGenerated
    public void setTotal(long total) {
        //DO NOTHING
    }

    @JsonProperty("took")
    @Deprecated(forRemoval = true)
    @JacocoGenerated
    public Long getTook() {
        return getProcessingTime();
    }

    @JsonProperty("took")
    @Deprecated(forRemoval = true)
    @JacocoGenerated
    public void setTook(long took) {
        //DO NOTHING
    }

    @JacocoGenerated
    @Override
    public int hashCode() {
        return Objects.hash(context, id, processingTime, size, hits);
    }

    @JacocoGenerated
    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }

        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        SearchResponseDto that = (SearchResponseDto) o;
        return processingTime == that.processingTime
               && size == that.size
               && Objects.equals(context, that.context)
               && Objects.equals(id, that.id)
               && Objects.equals(hits, that.hits);
    }

    private static List<JsonNode> extractSourcesList(SearchResponse searchResponse) {
        return Arrays.stream(searchResponse.getHits().getHits())
            .map(SearchHit::getSourceAsMap)
            .map(source -> objectMapperWithEmpty.convertValue(source, JsonNode.class))
            .collect(Collectors.toList());
    }

    private static JsonNode extractAggregations(SearchResponse searchResponse) {
        JsonNode json = attempt(() -> objectMapperWithEmpty.readTree(searchResponse.toString())).orElseThrow();

        ObjectNode aggregations = (ObjectNode) json.get("aggregations");

        if (aggregations == null) {
            return null;
        }

        return formatAggregations(aggregations);
    }

    public static JsonNode formatAggregations(JsonNode aggregations) {
        var outputAggregationNode = objectMapperWithEmpty.createObjectNode();

        var iterator = aggregations.fields();
        while (iterator.hasNext()) {
            var nodeEntry = iterator.next();
            var fieldName = nodeEntry.getKey();

            Optional<String> newName = Optional.ofNullable(AGGREGATION_FIELDS_TO_CHANGE.get(fieldName));
            if (newName.isEmpty()) {
                newName = Optional.of(fieldName.replaceFirst(WORD_ENDING_WITH_HASHTAG_REGEX, ""));
            }

            var value = nodeEntry.getValue();
            if (value.isValueNode()) {
                outputAggregationNode.set(newName.get(), value);
            } else if (value.isArray()) {
                var arrayNode = objectMapperWithEmpty.createArrayNode();
                value.forEach(element -> arrayNode.add(formatAggregations(element)));
                outputAggregationNode.set(newName.get(), arrayNode);
            } else {
                outputAggregationNode.set(newName.get(), formatAggregations(nodeEntry.getValue()));
            }
        }

        return outputAggregationNode;
    }

    public JsonNode getAggregations() {
        return aggregations;
    }

    public static final class Builder {

        private final SearchResponseDto response;

        public Builder() {
            response = new SearchResponseDto();
        }

        public Builder withContext(URI context) {
            response.setContext(context);
            return this;
        }

        public Builder withHits(List<JsonNode> hits) {
            response.setHits(hits);
            return this;
        }

        public Builder withId(URI id) {
            response.setId(id);
            return this;
        }

        public Builder withSize(long size) {
            response.setSize(size);
            return this;
        }

        public Builder withProcessingTime(long processingTime) {
            response.setProcessingTime(processingTime);
            return this;
        }

        public Builder withAggregations(JsonNode aggregations) {
            response.setAggregations(aggregations);
            return this;
        }

        public SearchResponseDto build() {
            return this.response;
        }
    }
}
