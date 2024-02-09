package no.unit.nva.search2.common.constant;

import java.util.stream.Stream;
import nva.commons.core.Environment;
import nva.commons.core.JacocoGenerated;
import org.opensearch.search.aggregations.AggregationBuilders;
import org.opensearch.search.aggregations.bucket.nested.NestedAggregationBuilder;
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
        return String.join(Words.DOT, args);
    }

    @JacocoGenerated
    public static String readSearchInfrastructureAuthUri() {
        return ENVIRONMENT.readEnv(SEARCH_INFRASTRUCTURE_AUTH_URI);
    }

    private static Stream<String> languageCodes() {
        return Stream.of(Words.BOKMAAL_CODE, Words.ENGLISH_CODE, Words.NYNORSK_CODE, Words.SAMI_CODE);
    }

    public static String readSearchInfrastructureApiUri() {
        return ENVIRONMENT.readEnv(SEARCH_INFRASTRUCTURE_API_URI);
    }

    public static String readApiHost() {
        return ENVIRONMENT.readEnv(API_HOST);
    }

    public static NestedAggregationBuilder topLevelOrganisationsHierarchy() {
        return
            nestedBranchBuilder(Words.TOP_LEVEL_ORGANIZATIONS, Words.TOP_LEVEL_ORGANIZATIONS)
                .subAggregation(
                    branchBuilder(Words.ID, Words.TOP_LEVEL_ORGANIZATIONS, Words.ID, Words.KEYWORD)
                        .subAggregation(
                            labels(Words.TOP_LEVEL_ORGANIZATIONS)
                        )
                );
    }

    public static NestedAggregationBuilder labels(String jsonPath) {
        var nestedAggregation =
            nestedBranchBuilder(Words.LABELS, jsonPath, Words.LABELS);

        languageCodes()
            .map(code -> branchBuilder(code, jsonPath, Words.LABELS, code, Words.KEYWORD))
            .forEach(nestedAggregation::subAggregation);

        return nestedAggregation;
    }

    public static TermsAggregationBuilder branchBuilder(String name, String... pathElements) {
        return AggregationBuilders
            .terms(name)
            .field(jsonPath(pathElements))
            .size(Defaults.DEFAULT_AGGREGATION_SIZE);
    }

    public static NestedAggregationBuilder nestedBranchBuilder(String name, String... pathElements) {
        return new NestedAggregationBuilder(name, jsonPath(pathElements));
    }
}
