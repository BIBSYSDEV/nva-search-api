package no.unit.nva.search.utils;

import org.mockito.ArgumentMatcher;
import org.opensearch.action.search.SearchRequest;
import org.opensearch.client.RequestOptions;

public class SortKeyHttpRequestMatcher implements ArgumentMatcher<SearchRequest> {

    private String expectedSortKey;

    public SortKeyHttpRequestMatcher(String expectedSortKey) {
        this.expectedSortKey = expectedSortKey;
    }

    @Override
    public boolean matches(SearchRequest searchRequest) {
        var searchRequestOrder = searchRequest.source().sorts().get(0).order();
        return searchRequestOrder.toString().equals(expectedSortKey);
    }
}
