package no.unit.nva.search2.constant;

import static no.unit.nva.search2.constant.Words.ADMINSTRATIVE_AGREEMENT;
import static no.unit.nva.search2.constant.Words.AFFILIATIONS;
import static no.unit.nva.search2.constant.Words.ASSOCIATED_ARTIFACTS;
import static no.unit.nva.search2.constant.Words.BOKMAAL_CODE;
import static no.unit.nva.search2.constant.Words.CONTEXT_TYPE;
import static no.unit.nva.search2.constant.Words.CONTRIBUTORS;
import static no.unit.nva.search2.constant.Words.DOI;
import static no.unit.nva.search2.constant.Words.DOT;
import static no.unit.nva.search2.constant.Words.ENGLISH_CODE;
import static no.unit.nva.search2.constant.Words.ENTITY_DESCRIPTION;
import static no.unit.nva.search2.constant.Words.FUNDINGS;
import static no.unit.nva.search2.constant.Words.FUNDING_SOURCE;
import static no.unit.nva.search2.constant.Words.ID;
import static no.unit.nva.search2.constant.Words.IDENTIFIER;
import static no.unit.nva.search2.constant.Words.IDENTITY;
import static no.unit.nva.search2.constant.Words.INSTANCE_TYPE;
import static no.unit.nva.search2.constant.Words.KEYWORD;
import static no.unit.nva.search2.constant.Words.LABELS;
import static no.unit.nva.search2.constant.Words.NAME;
import static no.unit.nva.search2.constant.Words.NYNORSK_CODE;
import static no.unit.nva.search2.constant.Words.ORC_ID;
import static no.unit.nva.search2.constant.Words.OWNER;
import static no.unit.nva.search2.constant.Words.OWNER_AFFILIATION;
import static no.unit.nva.search2.constant.Words.PUBLICATION_CONTEXT;
import static no.unit.nva.search2.constant.Words.PUBLICATION_DATE;
import static no.unit.nva.search2.constant.Words.PUBLICATION_INSTANCE;
import static no.unit.nva.search2.constant.Words.PUBLISHED_FILE;
import static no.unit.nva.search2.constant.Words.REFERENCE;
import static no.unit.nva.search2.constant.Words.RESOURCE_OWNER;
import static no.unit.nva.search2.constant.Words.SAMI_CODE;
import static no.unit.nva.search2.constant.Words.SOURCE;
import static no.unit.nva.search2.constant.Words.TOP_LEVEL_ORGANIZATION;
import static no.unit.nva.search2.constant.Words.TOP_LEVEL_ORGANIZATIONS;
import static no.unit.nva.search2.constant.Words.TYPE;
import static no.unit.nva.search2.constant.Words.USER;
import static no.unit.nva.search2.constant.Words.USER_AFFILIATION;
import static no.unit.nva.search2.constant.Words.YEAR;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import java.util.stream.Stream;
import no.unit.nva.commons.json.JsonUtils;
import nva.commons.core.Environment;
import nva.commons.core.JacocoGenerated;
import org.opensearch.search.aggregations.AbstractAggregationBuilder;
import org.opensearch.search.aggregations.AggregationBuilders;
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
    public static final String IDENTIFIER_KEYWORD = IDENTIFIER + DOT + KEYWORD;

    public static final String ENTITY_DESCRIPTION_CONTRIBUTORS_AFFILIATION_ID =
        jsonPath(ENTITY_DESCRIPTION, CONTRIBUTORS, AFFILIATIONS, ID, KEYWORD);

    public static final String ENTITY_DESCRIPTION_CONTRIBUTORS_AFFILIATION_LABELS =
        jsonPath(ENTITY_DESCRIPTION, CONTRIBUTORS, AFFILIATIONS, LABELS);

    public static final String ENTITY_DESCRIPTION_CONTRIBUTORS_IDENTITY_ID =
        jsonPath(ENTITY_DESCRIPTION, CONTRIBUTORS, IDENTITY, ID, KEYWORD);

    public static final String ENTITY_DESCRIPTION_CONTRIBUTORS_IDENTITY_NAME =
        jsonPath(ENTITY_DESCRIPTION, CONTRIBUTORS, IDENTITY, NAME, KEYWORD);

    public static final String ENTITY_DESCRIPTION_CONTRIBUTORS_IDENTITY_ORC_ID =
        jsonPath(ENTITY_DESCRIPTION, CONTRIBUTORS, IDENTITY, ORC_ID, KEYWORD);

    public static final String ENTITY_DESCRIPTION_PUBLICATION_DATE_YEAR =
        jsonPath(ENTITY_DESCRIPTION, PUBLICATION_DATE, YEAR);

    public static final String ENTITY_DESCRIPTION_REFERENCE_DOI =
        jsonPath(ENTITY_DESCRIPTION, REFERENCE, DOI, KEYWORD);
    public static final String ENTITY_DESCRIPTION_REFERENCE_PUBLICATION_CONTEXT_ISBN_LIST =
        jsonPath(ENTITY_DESCRIPTION, REFERENCE, PUBLICATION_CONTEXT, "isbnList");

    public static final String ENTITY_DESCRIPTION_REFERENCE_PUBLICATION_CONTEXT_ONLINE_ISSN =
        jsonPath(ENTITY_DESCRIPTION, REFERENCE, PUBLICATION_CONTEXT, "onlineIssn", KEYWORD);

    public static final String ENTITY_DESCRIPTION_REFERENCE_PUBLICATION_CONTEXT_PRINT_ISSN =
        jsonPath(ENTITY_DESCRIPTION, REFERENCE, PUBLICATION_CONTEXT, "printIssn", KEYWORD);

    public static final String ENTITY_DESCRIPTION_REFERENCE_PUBLICATION_CONTEXT_TYPE_KEYWORD =
        jsonPath(ENTITY_DESCRIPTION, REFERENCE, PUBLICATION_CONTEXT, TYPE, KEYWORD);

    public static final String ENTITY_DESCRIPTION_REFERENCE_PUBLICATION_INSTANCE_TYPE =
        jsonPath(ENTITY_DESCRIPTION, REFERENCE, PUBLICATION_INSTANCE, TYPE, KEYWORD);

    public static final String ENTITY_DESCRIPTION_REFERENCE_PUBLICATION_INSTANCE_TYPE_KEYWORD =
        jsonPath(ENTITY_DESCRIPTION, REFERENCE, PUBLICATION_INSTANCE, TYPE, KEYWORD);

    public static final String FUNDINGS_SOURCE_LABELS = jsonPath(FUNDINGS, SOURCE, LABELS);
    public static final String RESOURCE_OWNER_OWNER_AFFILIATION_KEYWORD =
        jsonPath(RESOURCE_OWNER, OWNER_AFFILIATION, KEYWORD);

    public static final String RESOURCE_OWNER_OWNER_KEYWORD = jsonPath(RESOURCE_OWNER, OWNER, KEYWORD);

    public static final List<AbstractAggregationBuilder<? extends AbstractAggregationBuilder<?>>>
        RESOURCES_AGGREGATIONS = List.of(
        generateSimpleAggregation(USER, RESOURCE_OWNER_OWNER_KEYWORD),
        generateSimpleAggregation(USER_AFFILIATION, RESOURCE_OWNER_OWNER_AFFILIATION_KEYWORD),
        generateSimpleAggregation(INSTANCE_TYPE, ENTITY_DESCRIPTION_REFERENCE_PUBLICATION_INSTANCE_TYPE_KEYWORD),
        generateSimpleAggregation(CONTEXT_TYPE, ENTITY_DESCRIPTION_REFERENCE_PUBLICATION_CONTEXT_TYPE_KEYWORD),
        generateFundingSourceAggregation(),
        generateObjectLabelsAggregation(TOP_LEVEL_ORGANIZATION, TOP_LEVEL_ORGANIZATIONS),
        generateHasFileAggregation()
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
