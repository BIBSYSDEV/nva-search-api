package no.unit.nva.search.models;

import no.unit.nva.search.SearchClient;
import no.unit.nva.search.restclients.responses.ViewingScope;
import org.opensearch.action.search.SearchRequest;
import org.opensearch.index.query.BoolQueryBuilder;
import org.opensearch.index.query.Operator;
import org.opensearch.index.query.QueryBuilder;
import org.opensearch.index.query.QueryBuilders;
import org.opensearch.search.aggregations.AbstractAggregationBuilder;
import org.opensearch.search.builder.SearchSourceBuilder;
import org.opensearch.search.sort.SortBuilders;
import org.opensearch.search.sort.SortOrder;

import java.net.URI;
import java.util.List;

public class SearchTicketsQuery {

    private static final String STRING = "string";
    private static final String OWNER_PROPERTY = "owner";
    private static final String OWNER_TICKETS_QUERY = "OwnerTicketsQuery";
    private final String searchTerm;
    private final int results;
    private final int from;
    private final String orderBy;
    private final SortOrder sortOrder;
    private final URI requestUri;
    private final List<AbstractAggregationBuilder<? extends AbstractAggregationBuilder<?>>> aggregations;

    public SearchTicketsQuery(
            String searchTerm,
            int results,
            int from,
            String orderBy,
            SortOrder sortOrder,
            URI requestUri,
            List<AbstractAggregationBuilder<? extends AbstractAggregationBuilder<?>>> aggregations) {
        this.searchTerm = searchTerm;
        this.results = results;
        this.from = from;
        this.orderBy = orderBy;
        this.sortOrder = sortOrder;
        this.requestUri = requestUri;
        this.aggregations = aggregations;
    }

    public SearchRequest createSearchRequestForTicketsWithOrganizationIds(
            ViewingScope viewingScope, String... indices) {
        return getSearchRequest(searchQueryBasedOnOrganizationIdsAndStatus(viewingScope), indices);
    }

    public SearchRequest createSearchRequestForTicketsByOwner(String owner, String... indices) {
        return getSearchRequest(searchQueryBasedUserAndStatus(owner), indices);
    }

    private SearchRequest getSearchRequest(BoolQueryBuilder queryBuilder, String[] indices) {
        SearchSourceBuilder searchSourceBuilder = new SearchSourceBuilder()
                .query(queryBuilder)
                .sort(
                        SortBuilders.fieldSort(orderBy).unmappedType(STRING).order(sortOrder))
                .size(results)
                .from(from)
                .trackTotalHits(true);

        if (aggregations != null) {
            addAggregations(searchSourceBuilder);
        }

        return new SearchRequest(indices).source(searchSourceBuilder);
    }

    private BoolQueryBuilder searchQueryBasedOnOrganizationIdsAndStatus(ViewingScope viewingScope) {
        return new BoolQueryBuilder()
                .should(generalSupportTickets(viewingScope))
                .should(doiRequestsForPublishedPublications(viewingScope))
                .should(publishingRequestsForDraftPublications(viewingScope));
    }

    private BoolQueryBuilder searchQueryBasedUserAndStatus(String owner) {
        return new BoolQueryBuilder()
                .must(QueryBuilders.queryStringQuery(searchTerm))
                .must(QueryBuilders.matchQuery(OWNER_PROPERTY, owner).operator(Operator.AND))
                .queryName(OWNER_TICKETS_QUERY)
                ;

    }

    private QueryBuilder publishingRequestsForDraftPublications(ViewingScope viewingScope) {
        BoolQueryBuilder queryBuilder = new BoolQueryBuilder()
                .must(QueryBuilders.matchQuery(SearchClient.DOCUMENT_TYPE, SearchClient.PUBLISHING_REQUEST))
                .must(QueryBuilders.matchQuery(SearchClient.PUBLICATION_STATUS, SearchClient.DRAFT_PUBLICATION_STATUS))
                .must(QueryBuilders.existsQuery(SearchClient.TICKET_STATUS))
                .must(QueryBuilders.queryStringQuery(searchTerm))
                .queryName(SearchClient.PUBLISHING_REQUESTS_QUERY_NAME);
        addViewingScope(viewingScope, queryBuilder);
        return queryBuilder;
    }

    private BoolQueryBuilder generalSupportTickets(ViewingScope viewingScope) {
        BoolQueryBuilder queryBuilder = new BoolQueryBuilder()
                .must(QueryBuilders.matchQuery(SearchClient.DOCUMENT_TYPE, SearchClient.GENERAL_SUPPORT_CASE))
                .must(QueryBuilders.existsQuery(SearchClient.ORGANIZATION_IDS))
                .must(QueryBuilders.existsQuery(SearchClient.TICKET_STATUS))
                .must(QueryBuilders.queryStringQuery(searchTerm))
                .queryName(SearchClient.GENERAL_SUPPORT_QUERY_NAME);
        addViewingScope(viewingScope, queryBuilder);
        return queryBuilder;
    }

    private BoolQueryBuilder doiRequestsForPublishedPublications(ViewingScope viewingScope) {
        BoolQueryBuilder queryBuilder = new BoolQueryBuilder()
                .must(QueryBuilders.matchQuery(SearchClient.DOCUMENT_TYPE, SearchClient.DOI_REQUEST))
                .must(QueryBuilders.existsQuery(SearchClient.ORGANIZATION_IDS))
                .must(QueryBuilders.existsQuery(SearchClient.TICKET_STATUS))
                .must(QueryBuilders.queryStringQuery(searchTerm))
                .mustNot(QueryBuilders.matchQuery(
                        SearchClient.PUBLICATION_STATUS, SearchClient.DRAFT_PUBLICATION_STATUS))
                .queryName(SearchClient.DOI_REQUESTS_QUERY_NAME);

        addViewingScope(viewingScope, queryBuilder);
        return queryBuilder;
    }

    private void addViewingScope(ViewingScope viewingScope, BoolQueryBuilder queryBuilder) {
        for (URI includedOrganizationId : viewingScope.getIncludedUnits()) {
            queryBuilder.must(
                            QueryBuilders.matchPhraseQuery(
                                    SearchClient.ORGANIZATION_IDS, includedOrganizationId.toString()))
                    .queryName(SearchClient.INCLUDED_VIEWING_SCOPES_QUERY_NAME);
        }
        for (URI excludedOrganizationId : viewingScope.getExcludedUnits()) {
            queryBuilder.mustNot(
                            QueryBuilders.matchPhraseQuery(
                                    SearchClient.ORGANIZATION_IDS, excludedOrganizationId.toString()))
                    .queryName(SearchClient.EXCLUDED_VIEWING_SCOPES_QUERY_NAME);
        }
    }

    private void addAggregations(SearchSourceBuilder sourceBuilder) {
        aggregations.forEach(sourceBuilder::aggregation);
    }

    public String getSearchTerm() {
        return searchTerm;
    }

    public URI getRequestUri() {
        return requestUri;
    }
}