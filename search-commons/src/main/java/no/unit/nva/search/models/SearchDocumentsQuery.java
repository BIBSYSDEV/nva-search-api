package no.unit.nva.search.models;

import org.opensearch.action.search.SearchRequest;
import org.opensearch.index.query.QueryBuilders;
import org.opensearch.search.builder.SearchSourceBuilder;
import org.opensearch.search.sort.SortBuilders;
import org.opensearch.search.sort.SortOrder;

import java.net.URI;

public class SearchDocumentsQuery {

    public static final String STRING = "string";
    private final String searchTerm;
    private final int results;
    private final int from;
    private final String orderBy;
    private final SortOrder sortOrder;
    private final URI requestUri;

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

    private SearchSourceBuilder toSearchSourceBuilder() {

        return new SearchSourceBuilder()
                .query(QueryBuilders.queryStringQuery(searchTerm))
                        .sort(SortBuilders.fieldSort(orderBy).unmappedType(STRING).order(sortOrder))
                        .from(from)
                        .size(results);
    }

    public SearchRequest toSearchRequest(String index) {
        return new SearchRequest(index).source(toSearchSourceBuilder());
    }
}
