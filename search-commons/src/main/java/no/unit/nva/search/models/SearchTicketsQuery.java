package no.unit.nva.search.models;

import static no.unit.nva.search.SearchClient.ID_FIELD;
import static no.unit.nva.search.SearchClient.ORGANIZATION_FIELD;
import static no.unit.nva.search.SearchClient.PART_OF_FIELD;
import java.net.URI;
import java.util.List;
import no.unit.nva.search.SearchClient;
import org.opensearch.action.search.SearchRequest;
import org.opensearch.index.query.BoolQueryBuilder;
import org.opensearch.index.query.Operator;
import org.opensearch.index.query.QueryBuilder;
import org.opensearch.index.query.QueryBuilders;
import org.opensearch.search.aggregations.AbstractAggregationBuilder;
import org.opensearch.search.builder.SearchSourceBuilder;
import org.opensearch.search.sort.SortBuilders;
import org.opensearch.search.sort.SortOrder;

public class SearchTicketsQuery {

    private static final CharSequence JSON_PATH_DELIMITER = ".";
    private static final String STRING = "string";
    private static final String OWNER_PROPERTY = "owner.username";
    private static final String OWNER_TICKETS_QUERY = "OwnerTicketsQuery";
    private static final String KEYWORD = "keyword";
    private final String searchTerm;
    private final int results;
    private final int from;
    private final String orderBy;
    private final SortOrder sortOrder;
    private final URI requestUri;
    private final List<AbstractAggregationBuilder<? extends AbstractAggregationBuilder<?>>> aggregations;
    private final List<String> viewingScope;
    private final boolean excludeSubUnits;

    public SearchTicketsQuery(
            String searchTerm,
            int results,
            int from,
            String orderBy,
            SortOrder sortOrder,
            URI requestUri,
            List<AbstractAggregationBuilder<? extends AbstractAggregationBuilder<?>>> aggregations,
            List<URI> viewingScope,
            boolean excludeSubUnits) {
        this.searchTerm = searchTerm;
        this.results = results;
        this.from = from;
        this.orderBy = orderBy;
        this.sortOrder = sortOrder;
        this.requestUri = requestUri;
        this.aggregations = aggregations;
        this.viewingScope = viewingScope.stream().map(URI::toString).toList();
        this.excludeSubUnits = excludeSubUnits;
    }

    public SearchRequest createSearchRequestForTicketsWithOrganizationIds(
            String... indices) {
        return getSearchRequest(searchQueryBasedOnOrganizationIdsAndStatus(), indices);
    }

    public SearchRequest createSearchRequestForTicketsByOwner(String owner, String... indices) {
        return getSearchRequest(searchQueryBasedUserAndStatus(owner), indices);
    }

    private SearchRequest getSearchRequest(BoolQueryBuilder queryBuilder, String... indices) {
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

    private BoolQueryBuilder searchQueryBasedOnOrganizationIdsAndStatus() {
        return new BoolQueryBuilder()
                   .must(viewingScopeQuery())
                   .must(new BoolQueryBuilder()
                             .should(generalSupportTickets())
                             .should(doiRequestsForPublishedPublications())
                             .should(publishingRequestsForPublications())
                   );
    }

    private BoolQueryBuilder searchQueryBasedUserAndStatus(String owner) {
        return new BoolQueryBuilder()
                .must(QueryBuilders.queryStringQuery(searchTerm))
                .must(QueryBuilders.matchQuery(OWNER_PROPERTY, owner).operator(Operator.AND))
                .queryName(OWNER_TICKETS_QUERY);

    }

    private QueryBuilder publishingRequestsForPublications() {
        BoolQueryBuilder queryBuilder = new BoolQueryBuilder()
            .must(QueryBuilders.matchQuery(SearchClient.DOCUMENT_TYPE, SearchClient.PUBLISHING_REQUEST))
            .must(QueryBuilders.existsQuery(SearchClient.TICKET_STATUS))
            .must(QueryBuilders.queryStringQuery(searchTerm))
            .queryName(SearchClient.PUBLISHING_REQUESTS_QUERY_NAME);
        return queryBuilder;
    }

    private BoolQueryBuilder generalSupportTickets() {
        BoolQueryBuilder queryBuilder = new BoolQueryBuilder()
                .must(QueryBuilders.matchQuery(SearchClient.DOCUMENT_TYPE, SearchClient.GENERAL_SUPPORT_CASE))
                .must(QueryBuilders.existsQuery(SearchClient.TICKET_STATUS))
                .must(QueryBuilders.queryStringQuery(searchTerm))
                .queryName(SearchClient.GENERAL_SUPPORT_QUERY_NAME);
        return queryBuilder;
    }

    private BoolQueryBuilder doiRequestsForPublishedPublications() {
        BoolQueryBuilder queryBuilder = new BoolQueryBuilder()
                .must(QueryBuilders.matchQuery(SearchClient.DOCUMENT_TYPE, SearchClient.DOI_REQUEST))
                .must(QueryBuilders.existsQuery(SearchClient.TICKET_STATUS))
                .must(QueryBuilders.queryStringQuery(searchTerm))
                .mustNot(QueryBuilders.matchQuery(
                        SearchClient.PUBLICATION_STATUS, SearchClient.DRAFT_PUBLICATION_STATUS))
                .queryName(SearchClient.DOI_REQUESTS_QUERY_NAME);

        return queryBuilder;
    }

    private QueryBuilder viewingScopeQuery() {
        return this.excludeSubUnits ? excludeSubUnitsQuery() : includeSubUnitsQuery();
    }

    private QueryBuilder includeSubUnitsQuery() {
        var query = QueryBuilders.boolQuery();
        query.should(QueryBuilders.termsQuery(jsonPathOf(ORGANIZATION_FIELD, ID_FIELD, KEYWORD), this.viewingScope));
        query.should(
            QueryBuilders.termsQuery(jsonPathOf(ORGANIZATION_FIELD, PART_OF_FIELD, KEYWORD),
                                               this.viewingScope));
        query.queryName(SearchClient.VIEWING_SCOPE_QUERY_NAME);
        return query;
    }

    private QueryBuilder excludeSubUnitsQuery() {
        return QueryBuilders.termsQuery(jsonPathOf(ORGANIZATION_FIELD, ID_FIELD, KEYWORD), this.viewingScope)
                   .queryName(SearchClient.VIEWING_SCOPE_QUERY_NAME);
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

    public List<String> getViewingScope() {
        return viewingScope;
    }

    private static String jsonPathOf(String... args) {
        return String.join(JSON_PATH_DELIMITER, args);
    }
}