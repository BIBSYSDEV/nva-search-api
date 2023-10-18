package no.unit.nva.search2.model.common;

import no.unit.nva.search2.ResourceAwsQuery;
import org.opensearch.index.query.AbstractQueryBuilder;

public record QueryBuilderWrapper(AbstractQueryBuilder<?> builder, ResourceAwsQuery query) {

}