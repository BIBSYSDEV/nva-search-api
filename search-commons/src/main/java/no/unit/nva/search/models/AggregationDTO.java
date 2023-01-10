package no.unit.nva.search.models;

import org.opensearch.search.aggregations.AggregationBuilders;
import org.opensearch.search.aggregations.bucket.terms.TermsAggregationBuilder;

public class AggregationDTO {

    public String term;
    public String field;
    public AggregationDTO subAggregation;
    public int aggregationBucketAmount = 100;

    public AggregationDTO(String term, String field) {
        this.term = term;
        this.field = field;
    }

    public AggregationDTO(String term, String field, AggregationDTO subAggregation) {
        this.term = term;
        this.field = field;
        this.subAggregation = subAggregation;
    }

    public AggregationDTO(String term, String field, AggregationDTO subAggregation, int aggregationBucketAmount) {
        this.term = term;
        this.field = field;
        this.subAggregation = subAggregation;
        this.aggregationBucketAmount = aggregationBucketAmount;
    }

    public TermsAggregationBuilder toAggregationBuilder() {
        return buildAggregations(this);
    }

    private static TermsAggregationBuilder buildAggregations(AggregationDTO aggDTO) {
        var aggregationBuilder = AggregationBuilders
            .terms(aggDTO.term)
            .field(aggDTO.field)
            .size(aggDTO.aggregationBucketAmount);

        if (aggDTO.subAggregation != null) {
            aggregationBuilder.subAggregation(buildAggregations(aggDTO.subAggregation));
        }

        return aggregationBuilder;
    }
}
