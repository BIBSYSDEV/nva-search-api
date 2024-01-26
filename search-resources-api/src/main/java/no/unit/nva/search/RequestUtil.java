package no.unit.nva.search;

import static nva.commons.core.attempt.Try.attempt;
import java.net.URI;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import no.unit.nva.search.models.CuratorSearchType;
import no.unit.nva.search.models.SearchDocumentsQuery;
import no.unit.nva.search.models.SearchTicketsQuery;
import nva.commons.apigateway.RequestInfo;
import nva.commons.core.JacocoGenerated;
import nva.commons.core.paths.UriWrapper;
import org.opensearch.search.aggregations.AbstractAggregationBuilder;
import org.opensearch.search.sort.SortOrder;

@JacocoGenerated
public class RequestUtil {

    public static final String SEARCH_TERM_KEY = "query";
    public static final String RESULTS_KEY = "results";
    public static final String FROM_KEY = "from";
    public static final String ORDERBY_KEY = "orderBy";
    public static final String VIEWING_SCOPE_KEY = "viewingScope";
    public static final String EXCLUDE_SUB_UNITS_KEY = "excludeSubUnits";
    public static final String SORTORDER_KEY = "sortOrder";
    private static final String RESULTS_DEFAULT_SIZE = "10";
    public static final String SEARCH_ALL_PUBLICATIONS_DEFAULT_QUERY = "*";
    private static final String ORDERBY_DEFAULT_POSITION = "modifiedDate";
    private static final String DEFAULT_SORT_ORDER = SortOrder.DESC.name();
    private static final String FROM_DEFAULT_POSITION = "0";
    public static final String PATH = "path";
    public static final String DOMAIN_NAME = "domainName";
    public static final String HTTPS = "https";
    private static final String COMMA = ",";
    public static final String FALSE = Boolean.FALSE.toString();

    /**
     * Get searchTerm from request query parameters.
     *
     * @param requestInfo requestInfo
     * @return searchTerm given in query parameter
     */
    public static String getSearchTerm(RequestInfo requestInfo) {
        return requestInfo.getQueryParameters().getOrDefault(SEARCH_TERM_KEY, SEARCH_ALL_PUBLICATIONS_DEFAULT_QUERY);
    }

    public static int getResults(RequestInfo requestInfo) {
        return Integer.parseInt(requestInfo.getQueryParameters().getOrDefault(RESULTS_KEY, RESULTS_DEFAULT_SIZE));
    }

    public static int getFrom(RequestInfo requestInfo) {
        return Integer.parseInt(requestInfo.getQueryParameters().getOrDefault(FROM_KEY, FROM_DEFAULT_POSITION));
    }

    public static String getOrderBy(RequestInfo requestInfo) {
        return requestInfo.getQueryParameters().getOrDefault(ORDERBY_KEY, ORDERBY_DEFAULT_POSITION);
    }

    public static SortOrder getSortOrder(RequestInfo requestInfo) {
        return SortOrder.fromString(requestInfo.getQueryParameters().getOrDefault(SORTORDER_KEY, DEFAULT_SORT_ORDER));
    }

    public static Optional<List<URI>> getViewingScope(RequestInfo requestInfo) {
        return requestInfo.getQueryParameterOpt(VIEWING_SCOPE_KEY)
                   .map(RequestUtil::splitStringToUris);
    }

    public static boolean getExcludeSubUnits(RequestInfo requestInfo) {
        return Boolean.parseBoolean(
            requestInfo.getQueryParameters().getOrDefault(EXCLUDE_SUB_UNITS_KEY, FALSE)
        );
    }

    public static URI getRequestUri(RequestInfo requestInfo) {
        String path = getRequestPath(requestInfo);
        String domainName = getRequestDomainName(requestInfo);
        return new UriWrapper(HTTPS, domainName).addChild(path).getUri();
    }

    private static List<URI> splitStringToUris(String s) {
        return Arrays.stream(s.split(COMMA)).map(URI::create).collect(Collectors.toList());
    }

    public static String getRequestPath(RequestInfo requestInfo) {
        return attempt(() -> requestInfo.getRequestContext()
            .get(PATH).asText())
            .orElseThrow();
    }

    public static String getRequestDomainName(RequestInfo requestInfo) {
        return attempt(() -> requestInfo.getRequestContext()
            .get(DOMAIN_NAME).asText())
            .orElseThrow();
    }

    public static SearchDocumentsQuery toQuery(
            RequestInfo requestInfo,
            List<AbstractAggregationBuilder<? extends AbstractAggregationBuilder<?>>> aggregations) {
        return new SearchDocumentsQuery(
            getSearchTerm(requestInfo),
            getResults(requestInfo),
            getFrom(requestInfo),
            getOrderBy(requestInfo),
            getSortOrder(requestInfo),
            getRequestUri(requestInfo),
            aggregations
        );
    }


    public static SearchTicketsQuery toQueryTickets(
            RequestInfo requestInfo,
            List<AbstractAggregationBuilder<? extends AbstractAggregationBuilder<?>>> aggregations) {
        return new SearchTicketsQuery(
            getSearchTerm(requestInfo),
            getResults(requestInfo),
            getFrom(requestInfo),
            getOrderBy(requestInfo),
            getSortOrder(requestInfo),
            getRequestUri(requestInfo),
            aggregations
        );
    }

    public static SearchTicketsQuery toQueryTicketsWithViewingScope(
        RequestInfo requestInfo,
        List<AbstractAggregationBuilder<? extends AbstractAggregationBuilder<?>>> aggregations,
        Set<CuratorSearchType> allowedSearchTypes) {
        var topLevelOrg = requestInfo.getTopLevelOrgCristinId().orElseThrow();
        return new SearchTicketsQuery(
            getSearchTerm(requestInfo),
            getResults(requestInfo),
            getFrom(requestInfo),
            getOrderBy(requestInfo),
            getSortOrder(requestInfo),
            getRequestUri(requestInfo),
            aggregations,
            getViewingScope(requestInfo).orElse(List.of(topLevelOrg)),
            allowedSearchTypes,
            getExcludeSubUnits(requestInfo)
        );
    }
}
