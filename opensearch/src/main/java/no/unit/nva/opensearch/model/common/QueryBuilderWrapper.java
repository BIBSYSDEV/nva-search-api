package no.unit.nva.opensearch.model.common;

import no.unit.nva.opensearch.ResourceAwsQuery;
import org.opensearch.index.query.AbstractQueryBuilder;

public record QueryBuilderWrapper(AbstractQueryBuilder<?> builder, ResourceAwsQuery query) {

}