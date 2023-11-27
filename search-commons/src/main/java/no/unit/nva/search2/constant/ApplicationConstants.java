package no.unit.nva.search2.constant;

import static no.unit.nva.search2.constant.ImportCandidateFields.COLLABORATION_TYPE;
import static no.unit.nva.search2.constant.ImportCandidateFields.IMPORT_STATUS;
import static no.unit.nva.search2.constant.ImportCandidateFields.PUBLICATION_INSTANCE_TYPE;
import static no.unit.nva.search2.constant.ImportCandidateFields.PUBLICATION_YEAR;
import static no.unit.nva.search2.constant.Words.ADMINSTRATIVE_AGREEMENT;
import static no.unit.nva.search2.constant.Words.ASSOCIATED_ARTIFACTS;
import static no.unit.nva.search2.constant.Words.BOKMAAL_CODE;
import static no.unit.nva.search2.constant.Words.CONTEXT_TYPE;
import static no.unit.nva.search2.constant.Words.DOT;
import static no.unit.nva.search2.constant.Words.ENGLISH_CODE;
import static no.unit.nva.search2.constant.Words.ENTITY_DESCRIPTION;
import static no.unit.nva.search2.constant.Words.FUNDINGS;
import static no.unit.nva.search2.constant.Words.FUNDING_SOURCE;
import static no.unit.nva.search2.constant.Words.ID;
import static no.unit.nva.search2.constant.Words.IDENTIFIER;
import static no.unit.nva.search2.constant.Words.INSTANCE_TYPE;
import static no.unit.nva.search2.constant.Words.KEYWORD;
import static no.unit.nva.search2.constant.Words.LABELS;
import static no.unit.nva.search2.constant.Words.NYNORSK_CODE;
import static no.unit.nva.search2.constant.Words.PUBLISHED_FILE;
import static no.unit.nva.search2.constant.Words.SAMI_CODE;
import static no.unit.nva.search2.constant.Words.SOURCE;
import static no.unit.nva.search2.constant.Words.TOP_LEVEL_ORGANIZATION;
import static no.unit.nva.search2.constant.Words.TOP_LEVEL_ORGANIZATIONS;
import static no.unit.nva.search2.constant.Words.TYPE;
import static no.unit.nva.search2.constant.Words.USER;
import static no.unit.nva.search2.constant.Words.USER_AFFILIATION;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import java.util.stream.Stream;
import no.unit.nva.commons.json.JsonUtils;
import nva.commons.core.Environment;
import nva.commons.core.JacocoGenerated;
import org.opensearch.index.query.TermQueryBuilder;
import org.opensearch.search.aggregations.AbstractAggregationBuilder;
import org.opensearch.search.aggregations.AggregationBuilders;
import org.opensearch.search.aggregations.bucket.filter.FilterAggregationBuilder;
import org.opensearch.search.aggregations.bucket.nested.NestedAggregationBuilder;
import org.opensearch.search.aggregations.bucket.terms.IncludeExclude;
import org.opensearch.search.aggregations.bucket.terms.TermsAggregationBuilder;


@JacocoGenerated
public final class ApplicationConstants {

    public static final ObjectMapper objectMapperWithEmpty = JsonUtils.dtoObjectMapper;
    public static final Environment ENVIRONMENT = new Environment();

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

    public static final String MAIN_TITLE = ENTITY_DESCRIPTION + DOT + "mainTitle";

    public static final List<AbstractAggregationBuilder<? extends AbstractAggregationBuilder<?>>>
        RESOURCES_AGGREGATIONS = List.of(
        generateSimpleAggregation(USER, ResourceFields.RESOURCE_OWNER_OWNER_KEYWORD),
        generateSimpleAggregation(USER_AFFILIATION, ResourceFields.RESOURCE_OWNER_OWNER_AFFILIATION_KEYWORD),
        generateSimpleAggregation(INSTANCE_TYPE,
                                  ResourceFields.ENTITY_DESCRIPTION_REFERENCE_PUBLICATION_INSTANCE_TYPE_KEYWORD),
        generateSimpleAggregation(CONTEXT_TYPE,
                                  ResourceFields.ENTITY_DESCRIPTION_REFERENCE_PUBLICATION_CONTEXT_TYPE_KEYWORD),
        generateFundingSourceAggregation(),
        generateObjectLabelsAggregation(TOP_LEVEL_ORGANIZATION, TOP_LEVEL_ORGANIZATIONS),
        generateHasFileAggregation()
    );

    public static final List<AbstractAggregationBuilder<? extends AbstractAggregationBuilder<?>>>
        IMPORT_CANDIDATES_AGGREGATIONS = List.of(
        generateSimpleAggregation("candidateStatus", IMPORT_STATUS),
        generateSimpleAggregation("publicationYear", PUBLICATION_YEAR),
        generateSimpleAggregation("instanceType", PUBLICATION_INSTANCE_TYPE),
        generateSimpleAggregation("collaborationType", COLLABORATION_TYPE),
        //        generateObjectLabelsAggregation("organization","organizations"),
        generateHasFileAggregation()

        //        generateImportedByUserAggregation()
    );

    private static TermsAggregationBuilder generateSimpleAggregation(String term, String field) {
        return AggregationBuilders
            .terms(term)
            .field(field)
            .size(Defaults.DEFAULT_AGGREGATION_SIZE);
    }

    private static NestedAggregationBuilder generateObjectLabelsAggregation(String name, String path) {
        return new NestedAggregationBuilder(name, path)
            .subAggregation(generateIdAggregation(path));
    }

    private static TermsAggregationBuilder generateFundingSourceAggregation() {
        return
            generateSimpleAggregation(FUNDING_SOURCE, jsonPath(FUNDINGS, SOURCE, IDENTIFIER, KEYWORD))
                .subAggregation(
                    generateLabelsAggregation(jsonPath(FUNDINGS, SOURCE)));
    }

    private static TermsAggregationBuilder generateIdAggregation(String object) {
        return new TermsAggregationBuilder(ID)
            .field(jsonPath(object, ID, KEYWORD))
            .size(Defaults.DEFAULT_AGGREGATION_SIZE)
            .subAggregation(generateLabelsAggregation(object));
    }

    private static NestedAggregationBuilder generateLabelsAggregation(String jsonPath) {
        var nestedAggregation = new NestedAggregationBuilder(LABELS, jsonPath(jsonPath, LABELS));
        Stream.of(BOKMAAL_CODE, ENGLISH_CODE, NYNORSK_CODE, SAMI_CODE)
            .map(code -> generateSimpleAggregation(code, jsonPath(jsonPath, LABELS, code, KEYWORD)))
            .forEach(nestedAggregation::subAggregation);
        return nestedAggregation;
    }

    private static FilterAggregationBuilder generateImportedByUserAggregation() {
        return new FilterAggregationBuilder(
            "importedByUser", new TermQueryBuilder("importStatus.candidateStatus.keyword", "IMPORTED"))
            .subAggregation(AggregationBuilders
                                .terms("importStatus.setBy")
                                .field("importStatus.setBy.keyword")
                                .size(Defaults.DEFAULT_AGGREGATION_SIZE));
    }


    private static TermsAggregationBuilder generateHasFileAggregation() {
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
