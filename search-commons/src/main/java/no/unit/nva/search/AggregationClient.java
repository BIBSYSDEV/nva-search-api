package no.unit.nva.search;

import org.opensearch.action.search.SearchRequest;
import org.opensearch.action.search.SearchResponse;
import org.opensearch.client.RequestOptions;
import org.opensearch.search.aggregations.AggregationBuilders;
import org.opensearch.search.builder.SearchSourceBuilder;

import java.util.Map;

import static nva.commons.core.attempt.Try.attempt;

public class AggregationClient {

    private final RestHighLevelClientWrapper openSearchClient;
    private final SearchSourceBuilder sourceBuilder = new SearchSourceBuilder();

    public AggregationClient(RestHighLevelClientWrapper restHighLevelClient) {
        this.openSearchClient = restHighLevelClient;
    }

    public SearchResponse aggregate(Map<String, String> termToField) {

        sourceBuilder.size(0);

        termToField.forEach(this::addAggregation);

        var searchRequest = new SearchRequest().source(sourceBuilder);

        return attempt(() ->
                openSearchClient.search(searchRequest, RequestOptions.DEFAULT)
            ).orElseThrow();
    }

    private void addAggregation(String term, String field) {
        var newAggregation = AggregationBuilders
                .terms(term)
                .field(field);

        sourceBuilder.aggregation(newAggregation);

    }
}
