package no.unit.nva.search.models;

import static java.util.Collections.emptyMap;
import static java.util.Objects.nonNull;
import java.net.URI;
import java.util.Map;
import org.opensearch.action.search.SearchRequest;
import org.opensearch.index.query.QueryBuilders;
import org.opensearch.search.aggregations.AggregationBuilders;
import org.opensearch.search.builder.SearchSourceBuilder;
import org.opensearch.search.sort.SortBuilders;
import org.opensearch.search.sort.SortOrder;

public class SearchDocumentsQuery {

    public static final String STRING = "string";
    private final String searchTerm;
    private final int results;
    private final int from;
    private final String orderBy;
    private final SortOrder sortOrder;
    private final URI requestUri;
    private Map<String, String> aggregationFields;
    private int aggregationBucketAmount = 100;

    public void setAggregationBucketAmount(int aggregationBucketAmount) {
        this.aggregationBucketAmount = aggregationBucketAmount;
    }

    public void setAggregationFields(Map<String, String> aggregationFields) {
        this.aggregationFields = aggregationFields;
    }

    public SearchDocumentsQuery(String searchTerm,
                                int results,
                                int from,
                                String orderBy,
                                SortOrder sortOrder,
                                URI requestUri,
                                Map<String, String> aggregationFields) {
        this.searchTerm = searchTerm;
        this.results = results;
        this.from = from;
        this.orderBy = orderBy;
        this.sortOrder = sortOrder;
        this.requestUri = requestUri;
        this.aggregationFields = nonNull(aggregationFields) ? aggregationFields : emptyMap();
    }

    public String getSearchTerm() {
        return searchTerm;
    }

    public URI getRequestUri() {
        return requestUri;
    }

    public SearchRequest toSearchRequest(String index) {
        return new SearchRequest(index).source(toSearchSourceBuilder());
    }

    private SearchSourceBuilder toSearchSourceBuilder() {

        var sourceBuilder = new SearchSourceBuilder()
                .query(QueryBuilders.queryStringQuery(searchTerm))
                .sort(SortBuilders.fieldSort(orderBy).unmappedType(STRING).order(sortOrder))
                .from(from)
                .size(results);

        if (aggregationFields != null) {
            addAggregationToSourceBuilder(sourceBuilder);
        }

        return sourceBuilder;
    }

    private void addAggregationToSourceBuilder(SearchSourceBuilder sourceBuilder) {
        aggregationFields.forEach((term, field) ->
                sourceBuilder.aggregation(
                        AggregationBuilders
                                .terms(term)
                                .field(field)
                                .size(aggregationBucketAmount)
                )
        );
    }
}
