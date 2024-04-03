package no.unit.nva.search2.common;

import org.opensearch.index.query.BoolQueryBuilder;
import org.opensearch.index.query.QueryBuilder;
import org.opensearch.index.query.QueryBuilders;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;


public class QueryFilter {
    private final transient HashMap<String, QueryBuilder> filters = new HashMap<>();

    public QueryFilter() {
    }

    public BoolQueryBuilder get() {
        var boolQueryBuilder = QueryBuilders.boolQuery();
        filters.values().forEach(boolQueryBuilder::must);
        return boolQueryBuilder;
    }

    public void set(QueryBuilder... filters) {
        this.filters.clear();
        var filterMap = Arrays.stream(filters)
            .map(filter -> Map.entry(filter.queryName(), filter))
            .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
        this.filters.putAll(filterMap);
    }

    public void add(QueryBuilder builder) {
        this.filters.put(builder.queryName(), builder);
    }
}
