package no.unit.nva.search.models;

import org.opensearch.search.aggregations.AggregationBuilders;
import org.opensearch.search.aggregations.bucket.terms.TermsAggregationBuilder;

public class AggregationDto {

    public String term;
    public String field;
    public AggregationDto subAggregation;
    public int aggregationBucketAmount = 100;

    public AggregationDto(String term, String field) {
        this.term = term;
        this.field = field;
    }

    public AggregationDto(String term, String field, AggregationDto subAggregation) {
        this.term = term;
        this.field = field;
        this.subAggregation = subAggregation;
    }

    public void setAggregationBucketAmount(int aggregationBucketAmount) {
        this.aggregationBucketAmount = aggregationBucketAmount;
    }

    public TermsAggregationBuilder toAggregationBuilder() {
        return buildAggregations(this);
    }

    private static TermsAggregationBuilder buildAggregations(AggregationDto aggDTO) {
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
