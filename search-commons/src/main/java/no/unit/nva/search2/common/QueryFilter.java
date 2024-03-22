package no.unit.nva.search2.common;

import org.opensearch.index.query.BoolQueryBuilder;
import org.opensearch.index.query.QueryBuilder;
import org.opensearch.index.query.QueryBuilders;

import java.util.ArrayList;
import java.util.List;

public class QueryFilter {
    private final transient List<QueryBuilder> filters = new ArrayList<>();

    public QueryFilter() {
    }

    public BoolQueryBuilder get() {
        var boolQueryBuilder = QueryBuilders.boolQuery();
        filters.forEach(boolQueryBuilder::must);
        return boolQueryBuilder;
    }

    public void set(QueryBuilder... filters) {
        this.filters.clear();
        this.filters.addAll(List.of(filters));
    }

    public void add(QueryBuilder builder) {
        this.filters.removeIf(filter -> filter.getName().equals(builder.getName()));
        this.filters.add(builder);
    }
}
