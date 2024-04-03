package no.unit.nva.search2.common;

import org.opensearch.index.query.BoolQueryBuilder;
import org.opensearch.index.query.QueryBuilder;
import org.opensearch.index.query.QueryBuilders;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;



public class QueryFilter {
    private final transient Map<String, QueryBuilder> filters = new HashMap<>();

    public QueryFilter() {
    }

    public BoolQueryBuilder get() {
        var boolQueryBuilder = QueryBuilders.boolQuery();
        filters.values().forEach(boolQueryBuilder::must);
        return boolQueryBuilder;
    }

    public void set(QueryBuilder... filters) {
        this.filters.clear();
        Arrays.stream(filters)
            .forEach(this::add);
    }

    public void add(QueryBuilder builder) {
        this.filters.put(builder.queryName(), builder);
    }
}
