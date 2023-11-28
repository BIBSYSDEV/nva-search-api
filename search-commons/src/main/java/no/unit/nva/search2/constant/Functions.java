package no.unit.nva.search2.constant;

import static no.unit.nva.search2.constant.Words.ADMINSTRATIVE_AGREEMENT;
import static no.unit.nva.search2.constant.Words.ASSOCIATED_ARTIFACTS;
import static no.unit.nva.search2.constant.Words.BOKMAAL_CODE;
import static no.unit.nva.search2.constant.Words.DOT;
import static no.unit.nva.search2.constant.Words.ENGLISH_CODE;
import static no.unit.nva.search2.constant.Words.ID;
import static no.unit.nva.search2.constant.Words.KEYWORD;
import static no.unit.nva.search2.constant.Words.LABELS;
import static no.unit.nva.search2.constant.Words.NYNORSK_CODE;
import static no.unit.nva.search2.constant.Words.PUBLISHED_FILE;
import static no.unit.nva.search2.constant.Words.SAMI_CODE;
import static no.unit.nva.search2.constant.Words.TYPE;
import java.util.stream.Stream;
import nva.commons.core.Environment;
import org.opensearch.search.aggregations.AggregationBuilders;
import org.opensearch.search.aggregations.bucket.nested.NestedAggregationBuilder;
import org.opensearch.search.aggregations.bucket.terms.IncludeExclude;
import org.opensearch.search.aggregations.bucket.terms.TermsAggregationBuilder;

public final class Functions {

    static final Environment ENVIRONMENT = new Environment();

    public static String jsonPath(String... args) {
        return String.join(DOT, args);
    }

    public static String readSearchInfrastructureAuthUri() {
        return ENVIRONMENT.readEnv("SEARCH_INFRASTRUCTURE_AUTH_URI");
    }

    public static String readSearchInfrastructureApiUri() {
        return ENVIRONMENT.readEnv("SEARCH_INFRASTRUCTURE_API_URI");
    }

    public static String readApiHost() {
        return ENVIRONMENT.readEnv("API_HOST");
    }

    static TermsAggregationBuilder generateSimpleAggregation(String term, String field) {
        return AggregationBuilders
            .terms(term)
            .field(field)
            .size(Defaults.DEFAULT_AGGREGATION_SIZE);
    }

    static NestedAggregationBuilder generateObjectLabelsAggregation(String name, String path) {
        return new NestedAggregationBuilder(name, path)
            .subAggregation(generateIdAggregation(path));
    }

    static TermsAggregationBuilder generateIdAggregation(String object) {
        return new TermsAggregationBuilder(ID)
            .field(jsonPath(object, ID, KEYWORD))
            .size(Defaults.DEFAULT_AGGREGATION_SIZE)
            .subAggregation(generateLabelsAggregation(object));
    }

    static NestedAggregationBuilder generateLabelsAggregation(String jsonPath) {
        var nestedAggregation = new NestedAggregationBuilder(LABELS, jsonPath(jsonPath, LABELS));
        Stream.of(BOKMAAL_CODE, ENGLISH_CODE, NYNORSK_CODE, SAMI_CODE)
            .map(code -> generateSimpleAggregation(code, jsonPath(jsonPath, LABELS, code, KEYWORD)))
            .forEach(nestedAggregation::subAggregation);
        return nestedAggregation;
    }

    static TermsAggregationBuilder generateHasFileAggregation() {
        var include = new IncludeExclude(PUBLISHED_FILE, "");
        return AggregationBuilders
            .terms(ASSOCIATED_ARTIFACTS)
            .field(jsonPath(ASSOCIATED_ARTIFACTS, TYPE, KEYWORD))
            .includeExclude(include)
            .size(Defaults.DEFAULT_AGGREGATION_SIZE)
            .subAggregation(
                generateSimpleAggregation("public", jsonPath(ASSOCIATED_ARTIFACTS, ADMINSTRATIVE_AGREEMENT))

            );
    }
}
