package no.unit.nva.search.models;

import java.net.URI;
import java.util.List;
import no.unit.nva.search.SearchClient;
import no.unit.nva.search.restclients.responses.ViewingScope;
import org.opensearch.action.search.SearchRequest;
import org.opensearch.index.query.BoolQueryBuilder;
import org.opensearch.index.query.QueryBuilder;
import org.opensearch.index.query.QueryBuilders;
import org.opensearch.search.builder.SearchSourceBuilder;

public class SearchTicketsQuery {

    public final int pageSize;
    public final int pageNo;
    private final List<AggregationDto> aggregations;

    public SearchTicketsQuery(int pageSize, int pageNo, List<AggregationDto> aggregations) {
        this.pageSize = pageSize;
        this.pageNo = pageNo;
        this.aggregations = aggregations;
    }

    public SearchRequest createSearchRequestForTicketsWithOrganizationIds(
        ViewingScope viewingScope, String... indices) {
        BoolQueryBuilder queryBuilder = searchQueryBasedOnOrganizationIdsAndStatus(viewingScope);
        SearchSourceBuilder searchSourceBuilder = new SearchSourceBuilder()
            .query(queryBuilder)
            .size(pageSize)
            .from(calculateFirstEntryIndex(pageSize, pageNo));

        if (aggregations != null) {
            addAggregations(searchSourceBuilder);
        }

        return new SearchRequest(indices).source(searchSourceBuilder);
    }

    private int calculateFirstEntryIndex(int pageSize, int pageNo) {
        return pageSize * pageNo;
    }

    private BoolQueryBuilder searchQueryBasedOnOrganizationIdsAndStatus(ViewingScope viewingScope) {
        return new BoolQueryBuilder()
            .should(allPendingGeneralSupportTickets(viewingScope))
            .should(allPendingDoiRequestsForPublishedPublications(viewingScope))
            .should(pendingPublishingRequestsForDraftPublications(viewingScope));
    }

    private QueryBuilder pendingPublishingRequestsForDraftPublications(ViewingScope viewingScope) {
        BoolQueryBuilder queryBuilder = new BoolQueryBuilder()
            .must(QueryBuilders.matchQuery(SearchClient.DOCUMENT_TYPE, SearchClient.PUBLISHING_REQUEST))
            .must(QueryBuilders.matchQuery(SearchClient.PUBLICATION_STATUS, SearchClient.DRAFT_PUBLICATION_STATUS))
            .must(QueryBuilders.matchQuery(SearchClient.TICKET_STATUS, SearchClient.PENDING))
            .queryName(SearchClient.PUBLISHING_REQUESTS_QUERY_NAME);
        addViewingScope(viewingScope, queryBuilder);
        return queryBuilder;
    }

    private BoolQueryBuilder allPendingGeneralSupportTickets(ViewingScope viewingScope) {
        BoolQueryBuilder queryBuilder = new BoolQueryBuilder()
            .must(QueryBuilders.matchQuery(SearchClient.DOCUMENT_TYPE, SearchClient.GENERAL_SUPPORT_CASE))
            .must(QueryBuilders.existsQuery(SearchClient.ORGANIZATION_IDS))
            .must(QueryBuilders.matchQuery(SearchClient.TICKET_STATUS, SearchClient.PENDING))
            .queryName(SearchClient.GENERAL_SUPPORT_QUERY_NAME);
        addViewingScope(viewingScope, queryBuilder);
        return queryBuilder;
    }

    private BoolQueryBuilder allPendingDoiRequestsForPublishedPublications(ViewingScope viewingScope) {
        BoolQueryBuilder queryBuilder = new BoolQueryBuilder()
            .must(QueryBuilders.matchQuery(SearchClient.DOCUMENT_TYPE, SearchClient.DOI_REQUEST))
            .must(QueryBuilders.existsQuery(SearchClient.ORGANIZATION_IDS))
            .must(QueryBuilders.matchQuery(SearchClient.TICKET_STATUS, SearchClient.PENDING))
            .mustNot(QueryBuilders.matchQuery(SearchClient.PUBLICATION_STATUS, SearchClient.DRAFT_PUBLICATION_STATUS))
            .queryName(SearchClient.DOI_REQUESTS_QUERY_NAME);

        addViewingScope(viewingScope, queryBuilder);
        return queryBuilder;
    }

    private void addViewingScope(ViewingScope viewingScope, BoolQueryBuilder queryBuilder) {
        for (URI includedOrganizationId : viewingScope.getIncludedUnits()) {
            queryBuilder.must(
                    QueryBuilders.matchPhraseQuery(SearchClient.ORGANIZATION_IDS, includedOrganizationId.toString()))
                .queryName(SearchClient.INCLUDED_VIEWING_SCOPES_QUERY_NAME);
        }
        for (URI excludedOrganizationId : viewingScope.getExcludedUnits()) {
            queryBuilder.mustNot(
                    QueryBuilders.matchPhraseQuery(SearchClient.ORGANIZATION_IDS, excludedOrganizationId.toString()))
                .queryName(SearchClient.EXCLUDED_VIEWING_SCOPES_QUERY_NAME);
        }
    }

    private void addAggregations(SearchSourceBuilder sourceBuilder) {
        aggregations.forEach(aggDTO -> sourceBuilder.aggregation(aggDTO.toAggregationBuilder()));
    }
}