package no.unit.nva.search.common;

import static no.unit.nva.constants.Words.COMMA;

import org.opensearch.index.query.BoolQueryBuilder;
import org.opensearch.index.query.QueryBuilder;
import org.opensearch.index.query.QueryBuilders;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * QueryFilter is a class that represents a query filter to the search service.
 *
 * @author Stig Norland
 */
public class QueryFilter {
    private static final String SUFFIX = "]";
    private static final String PREFIX = "[";

    private final transient Map<String, QueryBuilder> filters = new HashMap<>();

    public QueryFilter() {}

    public BoolQueryBuilder get() {
        var boolQueryBuilder = QueryBuilders.boolQuery();
        filters.values().forEach(boolQueryBuilder::must);
        return boolQueryBuilder;
    }

    /**
     * Clears filter and sets new filters.
     *
     * @param filters QueryBuilder
     */
    public void set(QueryBuilder... filters) {
        this.filters.clear();
        Arrays.stream(filters).forEach(this::add);
    }

    public boolean hasContent() {
        return !filters.isEmpty();
    }

    public QueryFilter add(QueryBuilder builder) {
        this.filters.put(builder.queryName(), builder);
        return this;
    }

    @Override
    public String toString() {
        return filters.values().stream()
                .map(QueryBuilder::toString)
                .collect(Collectors.joining(COMMA, PREFIX, SUFFIX));
    }
}
