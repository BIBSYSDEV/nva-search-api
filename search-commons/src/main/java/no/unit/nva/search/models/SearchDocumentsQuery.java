package no.unit.nva.search.models;

import java.net.URI;
import java.util.List;
import org.opensearch.action.search.SearchRequest;
import org.opensearch.index.query.QueryBuilders;
import org.opensearch.search.builder.SearchSourceBuilder;
import org.opensearch.search.sort.SortBuilders;
import org.opensearch.search.sort.SortOrder;

public class SearchDocumentsQuery {

    public static final String STRING = "string";
    private final String searchTerm;
    private final String filterTerm;
    private final int results;
    private final int from;
    private final String orderBy;
    private final SortOrder sortOrder;
    private final URI requestUri;
    private final List<AggregationDto> aggregations;

    public SearchDocumentsQuery(String searchTerm,
                                String filterTerm,
                                int results,
                                int from,
                                String orderBy,
                                SortOrder sortOrder,
                                URI requestUri,
                                List<AggregationDto> aggregations) {
        this.searchTerm = searchTerm;
        this.filterTerm = filterTerm;
        this.results = results;
        this.from = from;
        this.orderBy = orderBy;
        this.sortOrder = sortOrder;
        this.requestUri = requestUri;
        this.aggregations = aggregations;
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
                                .postFilter(QueryBuilders.queryStringQuery(filterTerm))
                                .sort(SortBuilders.fieldSort(orderBy).unmappedType(STRING).order(sortOrder))
                                .from(from)
                                .size(results)
                                .trackTotalHits(true);

        if (aggregations != null) {
            addAggregations(sourceBuilder);
        }

        return sourceBuilder;
    }

    private void addAggregations(SearchSourceBuilder sourceBuilder) {
        aggregations.forEach(aggDTO -> sourceBuilder.aggregation(aggDTO.toAggregationBuilder()));
    }
}
