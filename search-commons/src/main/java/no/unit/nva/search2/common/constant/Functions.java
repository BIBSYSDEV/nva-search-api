package no.unit.nva.search2.common.constant;

import static no.unit.nva.search2.common.constant.Words.BOKMAAL_CODE;
import static no.unit.nva.search2.common.constant.Words.DOT;
import static no.unit.nva.search2.common.constant.Words.ENGLISH_CODE;
import static no.unit.nva.search2.common.constant.Words.ID;
import static no.unit.nva.search2.common.constant.Words.KEYWORD;
import static no.unit.nva.search2.common.constant.Words.LABELS;
import static no.unit.nva.search2.common.constant.Words.NYNORSK_CODE;
import static no.unit.nva.search2.common.constant.Words.SAMI_CODE;
import static no.unit.nva.search2.common.constant.Words.TOP_LEVEL_ORGANIZATION;
import static no.unit.nva.search2.common.constant.Words.TOP_LEVEL_ORGANIZATIONS;
import java.util.stream.Stream;
import nva.commons.core.Environment;
import nva.commons.core.JacocoGenerated;
import org.opensearch.index.query.QueryBuilders;
import org.opensearch.search.aggregations.AggregationBuilders;
import org.opensearch.search.aggregations.bucket.filter.FilterAggregationBuilder;
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
        return String.join(DOT, args);
    }

    @JacocoGenerated
    public static String readSearchInfrastructureAuthUri() {
        return ENVIRONMENT.readEnv(SEARCH_INFRASTRUCTURE_AUTH_URI);
    }

    private static Stream<String> languageCodes() {
        return Stream.of(BOKMAAL_CODE, ENGLISH_CODE, NYNORSK_CODE, SAMI_CODE);
    }

    public static String readSearchInfrastructureApiUri() {
        return ENVIRONMENT.readEnv(SEARCH_INFRASTRUCTURE_API_URI);
    }

    public static String readApiHost() {
        return ENVIRONMENT.readEnv(API_HOST);
    }

    public static NestedAggregationBuilder topLevelOrganisationsHierarchy() {
        return
            nestedBranchBuilder(TOP_LEVEL_ORGANIZATION, TOP_LEVEL_ORGANIZATIONS)
                .subAggregation(
                    branchBuilder(ID, TOP_LEVEL_ORGANIZATIONS, ID, KEYWORD)
                        .subAggregation(
                            labels(TOP_LEVEL_ORGANIZATIONS)
                        )
                );
    }

    public static NestedAggregationBuilder labels(String jsonPath) {
        var nestedAggregation =
            nestedBranchBuilder(LABELS, jsonPath, LABELS);

        languageCodes()
            .map(code -> branchBuilder(code, jsonPath, LABELS, code, KEYWORD))
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

    public static FilterAggregationBuilder filterBranchBuilder(String name, String filter, String... paths) {
        return AggregationBuilders.filter(name, QueryBuilders.termQuery(jsonPath(paths), filter));
    }
}
