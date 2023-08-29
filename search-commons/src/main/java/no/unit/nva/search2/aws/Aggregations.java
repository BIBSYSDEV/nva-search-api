package no.unit.nva.search2.aws;

import java.util.Map;
import org.opensearch.client.opensearch._types.aggregations.Aggregation;
import org.opensearch.client.opensearch._types.query_dsl.BoolQuery.Builder;
import org.opensearch.client.opensearch._types.query_dsl.Query;
import org.opensearch.client.opensearch._types.query_dsl.QueryStringQuery;

public final class Aggregations {

    private static final String AFFILIATIONS = "affiliations";
    private static final String APPROVAL_STATUS = "approvalStatus";
    private static final String PENDING = "pending";
    private static final String APPROVED = "approved";
    private static final String REJECTED = "rejected";
    private static final CharSequence JSON_PATH_DELIMITER = ".";
    public static final Map<String, Aggregation> AGGREGATIONS_MAP = Map.of(
        jsonPathOf(APPROVAL_STATUS, PENDING), pendingAggregation(),
        jsonPathOf(APPROVAL_STATUS, APPROVED), approvedAggregation(),
        jsonPathOf(APPROVAL_STATUS, REJECTED), rejectedAggregation()
    );

    private Aggregations() {
    }

    private static String jsonPathOf(String... args) {
        return String.join(JSON_PATH_DELIMITER, args);
    }

    private static Aggregation approvedAggregation() {
        return new Aggregation.Builder()
                   .filter(queryNotToMatchApprovalStatusPendingAndRejected())
                   .build();
    }

    private static Query queryNotToMatchApprovalStatusPendingAndRejected() {
        return new Builder()
                   .mustNot(queryToMatchApprovalStatus("PENDING"),
                            queryToMatchApprovalStatus("REJECTED"))
                   .build()._toQuery();
    }

    private static Aggregation pendingAggregation() {
        return new Aggregation.Builder()
                   .filter(queryToMatchApprovalStatus("PENDING"))
                   .build();
    }

    private static Aggregation rejectedAggregation() {
        return new Aggregation.Builder()
                   .filter(queryToMatchApprovalStatus("REJECTED"))
                   .build();
    }

    private static Query queryToMatchApprovalStatus(String approvalStatus) {
        return new Query(new QueryStringQuery.Builder()
                             .query(approvalStatus)
                             .defaultField(jsonPathOf(AFFILIATIONS, APPROVAL_STATUS))
                             .build());
    }
}
