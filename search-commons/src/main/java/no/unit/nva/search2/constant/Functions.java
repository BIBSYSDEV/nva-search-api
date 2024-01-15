package no.unit.nva.search2.constant;

import static java.util.Objects.nonNull;
import static no.unit.nva.search2.constant.Words.BOKMAAL_CODE;
import static no.unit.nva.search2.constant.Words.DOT;
import static no.unit.nva.search2.constant.Words.ENGLISH_CODE;
import static no.unit.nva.search2.constant.Words.ID;
import static no.unit.nva.search2.constant.Words.KEYWORD;
import static no.unit.nva.search2.constant.Words.LABELS;
import static no.unit.nva.search2.constant.Words.NYNORSK_CODE;
import static no.unit.nva.search2.constant.Words.SAMI_CODE;
import static no.unit.nva.search2.constant.Words.TOP_LEVEL_ORGANIZATIONS;
import java.util.stream.Stream;
import nva.commons.core.Environment;
import nva.commons.core.JacocoGenerated;
import org.opensearch.search.aggregations.AggregationBuilders;
import org.opensearch.search.aggregations.bucket.nested.NestedAggregationBuilder;
import org.opensearch.search.aggregations.bucket.terms.IncludeExclude;
import org.opensearch.search.aggregations.bucket.terms.TermsAggregationBuilder;

public final class Functions {

    static final Environment ENVIRONMENT = new Environment();
    private static final String SEARCH_INFRASTRUCTURE_AUTH_URI = "SEARCH_INFRASTRUCTURE_AUTH_URI";
    private static final String SEARCH_INFRASTRUCTURE_API_URI = "SEARCH_INFRASTRUCTURE_API_URI";
    private static final String API_HOST = "API_HOST";

    @JacocoGenerated
    public Functions() {
    }

    public static String jsonPath(String... args) {
        return String.join(DOT, args);
    }

    @JacocoGenerated
    public static String readSearchInfrastructureAuthUri() {
        return ENVIRONMENT.readEnv(SEARCH_INFRASTRUCTURE_AUTH_URI);
    }

    public static String readSearchInfrastructureApiUri() {
        return ENVIRONMENT.readEnv(SEARCH_INFRASTRUCTURE_API_URI);
    }

    public static String readApiHost() {
        return ENVIRONMENT.readEnv(API_HOST);
    }

    public static NestedAggregationBuilder generateTopLevelOrganisationAggregation() {
        return
            splittedBranchBuilder(TOP_LEVEL_ORGANIZATIONS, TOP_LEVEL_ORGANIZATIONS)
                .subAggregation(
                    branchBuilder(ID, TOP_LEVEL_ORGANIZATIONS, ID, KEYWORD)
                        .subAggregation(generateLabelsAggregation(TOP_LEVEL_ORGANIZATIONS))
                );
    }

    public static NestedAggregationBuilder generateLabelsAggregation(String jsonPath) {
        var nestedAggregation = splittedBranchBuilder(LABELS, jsonPath, LABELS);
        Stream.of(BOKMAAL_CODE, ENGLISH_CODE, NYNORSK_CODE, SAMI_CODE)
            .map(code -> branchBuilder(code, jsonPath, LABELS, code, KEYWORD))
            .forEach(nestedAggregation::subAggregation);
        return nestedAggregation;
    }

    public static TermsAggregationBuilder branchBuilder(String name, IncludeExclude include, String... pathElements) {
        var builder = branchBuilder(name, pathElements);
        if (nonNull(include)) {
            builder.includeExclude(include);
        }
        return builder;
    }

    public static TermsAggregationBuilder branchBuilder(String name, String... pathElements) {
        return AggregationBuilders
            .terms(name)
            .field(jsonPath(pathElements))
            .size(Defaults.DEFAULT_AGGREGATION_SIZE);
    }

    public static NestedAggregationBuilder splittedBranchBuilder(String name, String... pathElements) {
        return new NestedAggregationBuilder(name, jsonPath(pathElements));
    }
}
