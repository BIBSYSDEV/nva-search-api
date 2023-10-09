package no.unit.nva.search2.model;

import no.unit.nva.search2.ResourceAwsQuery;
import org.opensearch.index.query.QueryStringQueryBuilder;

public record QueryBuilderWrapper(QueryStringQueryBuilder builder, ResourceAwsQuery query) {

}