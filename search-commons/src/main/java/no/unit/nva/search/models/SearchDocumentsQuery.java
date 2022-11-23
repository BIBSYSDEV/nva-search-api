package no.unit.nva.search.models;

import org.opensearch.action.search.SearchRequest;
import org.opensearch.index.query.BoolQueryBuilder;
import org.opensearch.index.query.QueryBuilder;
import org.opensearch.index.query.QueryBuilders;
import org.opensearch.search.aggregations.AggregationBuilders;
import org.opensearch.search.builder.SearchSourceBuilder;
import org.opensearch.search.sort.SortBuilders;
import org.opensearch.search.sort.SortOrder;

import java.net.URI;
import java.util.Map;

import static no.unit.nva.search.constants.ApplicationConstants.AGGREGATIONS;

public class SearchDocumentsQuery {

    public static final String STRING = "string";
    private final String searchTerm;
    private final int results;
    private final int from;
    private final String orderBy;
    private final SortOrder sortOrder;
    private final URI requestUri;

    public void setAggregationFields(Map<String, String> aggregationFields) {
        this.aggregationFields = aggregationFields;
    }

    private Map<String, String> aggregationFields;

    public SearchDocumentsQuery(String searchTerm,
                                int results,
                                int from,
                                String orderBy,
                                SortOrder sortOrder,
                                URI requestUri) {
        this.searchTerm = searchTerm;
        this.results = results;
        this.from = from;
        this.orderBy = orderBy;
        this.sortOrder = sortOrder;
        this.requestUri = requestUri;
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
                )
        );
    }
}
