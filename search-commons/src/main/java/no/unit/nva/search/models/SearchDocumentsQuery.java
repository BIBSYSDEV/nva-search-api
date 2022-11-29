package no.unit.nva.search.models;

import org.opensearch.action.search.SearchRequest;
import org.opensearch.index.query.QueryBuilders;
import org.opensearch.search.aggregations.AggregationBuilders;
import org.opensearch.search.aggregations.bucket.terms.TermsAggregationBuilder;
import org.opensearch.search.builder.SearchSourceBuilder;
import org.opensearch.search.sort.SortBuilders;
import org.opensearch.search.sort.SortOrder;

import java.net.URI;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static java.util.Collections.emptyMap;
import static java.util.Objects.nonNull;

public class SearchDocumentsQuery {

    public static final String STRING = "string";
    private final String searchTerm;
    private final int results;
    private final int from;
    private final String orderBy;
    private final SortOrder sortOrder;
    private final URI requestUri;

    private final Map<String, String> aggregationFields;

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

        var sourceBuilder = getSearchSourceBuilder();
        collectAggregations()
                .forEach(sourceBuilder::aggregation);
        return sourceBuilder;
    }

    private SearchSourceBuilder getSearchSourceBuilder() {
        return new SearchSourceBuilder()
                .query(QueryBuilders.queryStringQuery(searchTerm))
                .sort(SortBuilders.fieldSort(orderBy).unmappedType(STRING).order(sortOrder))
                .from(from)
                .size(results);
    }

    private List<TermsAggregationBuilder> collectAggregations() {
        return aggregationFields.entrySet().stream()
                .map(entry -> AggregationBuilders.terms(entry.getKey()).field(entry.getValue()))
                .collect(Collectors.toList());
    }
}
