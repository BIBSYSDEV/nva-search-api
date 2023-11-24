package no.unit.nva.search2.constant;

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

    public static final Integer EXPECTED_TWO_PARTS = 2;
    public static final String AFFILIATIONS = "affiliations";
    public static final String ALL = "all";
    public static final String AMPERSAND = "&";
    public static final String ASTERISK = "*";
    public static final String BOKMAAL_CODE = "nb";
    public static final String COLON = ":";
    public static final String COMMA = ",";
    public static final String CONTEXT_TYPE = "contextType";
    public static final String CONTRIBUTORS = "contributors";
    public static final String DOI = "doi";
    public static final String DOT = ".";
    public static final String ENGLISH_CODE = "en";
    public static final String ENTITY_DESCRIPTION = "entityDescription";
    public static final String EQUAL = "=";
    public static final String FUNDINGS = "fundings";
    public static final String FUNDING_SOURCE = "fundingSource";
    public static final String ID = "id";
    public static final String IDENTIFIER = "identifier";
    public static final String IDENTITY = "identity";
    public static final String INSTANCE_TYPE = "instanceType";
    public static final String JANUARY_FIRST = "-01-01";
    public static final String KEYWORD = "keyword";
    public static final String LABELS = "labels";
    public static final String NAME = "name";
    public static final String NYNORSK_CODE = "nn";
    public static final String ORC_ID = "orcId";
    public static final String OWNER = "owner";
    public static final String OWNER_AFFILIATION = "ownerAffiliation";
    public static final String PIPE = "|";
    public static final String PLUS = "+";
    public static final String PREFIX = "(";
    public static final String PUBLICATION_CONTEXT = "publicationContext";
    public static final String PUBLICATION_DATE = "publicationDate";
    public static final String PUBLICATION_INSTANCE = "publicationInstance";
    public static final String QUOTE = "'";
    public static final String REFERENCE = "reference";
    public static final String RESOURCES = "resources";
    public static final String RESOURCE_OWNER = "resourceOwner";
    public static final String SAMI_CODE = "sme";
    public static final String SEARCH = "_search";
    public static final String SEARCH_INFRASTRUCTURE_CREDENTIALS = "SearchInfrastructureCredentials";
    public static final String SOURCE = "source";
    public static final String SPACE = " ";
    public static final String SUFFIX = ")";
    public static final String TOP_LEVEL_ORGANIZATION = "topLevelOrganization";
    public static final String TOP_LEVEL_ORGANIZATIONS = "topLevelOrganizations";
    public static final String TYPE = "type";
    public static final String UNDERSCORE = "_";
    public static final String USER = "user";
    public static final String USER_AFFILIATION = "userAffiliation";
    public static final String YEAR = "year";
    public static final String ZERO = "0";
    public static final String MODIFIED_DATE = "modifiedDate";
    public static final String PROJECTS_ID = "projects.id";
    public static final String PUBLISHED_DATE = "publishedDate";
    public static final String ASSOCIATED_ARTIFACTS = "associatedArtifacts";
    public static final String PUBLISHED_FILE = "PublishedFile";
    public static final String ADMINSTRATIVE_AGREEMENT = "administrativeAgreement";

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
        generateTopLevelOrganizationAggregation2(),
        //        generateSimpleAggregation(ASSOCIATED_ARTIFACTS, ASSOCIATED_ARTIFACTS + DOT + TYPE + DOT + KEYWORD).,
//        generateHasFileAggregation(),
        generateObjectLabelsAggregation(TOP_LEVEL_ORGANIZATION, TOP_LEVEL_ORGANIZATIONS)
        //        generateIdAggregation(TOP_LEVEL_ORGANIZATION, TOP_LEVEL_ORGANIZATIONS)
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

    private static TermsAggregationBuilder generateTopLevelOrganizationAggregation2() {
        return
            generateSimpleAggregation(TOP_LEVEL_ORGANIZATION, TOP_LEVEL_ORGANIZATIONS + DOT + ID + DOT + KEYWORD)
                .subAggregation(
                    generateLabelsAggregation(TOP_LEVEL_ORGANIZATIONS));
    }

    private static NestedAggregationBuilder generateTopLevelAggregation() {
        return
            new NestedAggregationBuilder("test1", jsonPath(TOP_LEVEL_ORGANIZATIONS))
                //                .subAggregation(
                .subAggregation(generateLabelsAggregation(jsonPath(TOP_LEVEL_ORGANIZATIONS)));
    }

    private static TermsAggregationBuilder generateTopLevelOrganizationAggregation() {
        return
            new TermsAggregationBuilder("test2")
                .field(jsonPath(TOP_LEVEL_ORGANIZATIONS, ID, KEYWORD))
                .subAggregation(generateLabelsAggregation(jsonPath(TOP_LEVEL_ORGANIZATIONS))
                );
    }

    private static TermsAggregationBuilder generateIdAggregation(String object) {
        return new TermsAggregationBuilder(ID)
            .field(jsonPath(object, ID, KEYWORD))
            .size(Defaults.DEFAULT_AGGREGATION_SIZE)
            .subAggregation(generateLabelsAggregation(object));
    }

    //    private static TermsAggregationBuilder generateIdAggregation(String displayName, String path) {
    //        return generateSimpleAggregation(displayName, jsonPath(path, ID, KEYWORD))
    //            .size(Defaults.DEFAULT_AGGREGATION_SIZE)
//            .subAggregation(generateLabelsAggregation(path));
    //    }

    private static NestedAggregationBuilder generateLabelsAggregation(String jsonPath) {
        var nestedAggregation = new NestedAggregationBuilder(LABELS, jsonPath(jsonPath, LABELS));
        Stream.of(BOKMAAL_CODE, ENGLISH_CODE, NYNORSK_CODE, SAMI_CODE)
            .map(code -> generateSimpleAggregation(code, jsonPath(jsonPath, LABELS, code, KEYWORD)))
            .forEach(nestedAggregation::subAggregation);
        return nestedAggregation;
    }

    private static TermsAggregationBuilder generateHasFileAggregation() {
        //        var publishedFileQuery = new TermQueryBuilder(jsonPath(ASSOCIATED_ARTIFACTS, TYPE, KEYWORD),
        //        PUBLISHED_FILE);
        //        var notAdministrativeAgreementQuery =
        //            new TermQueryBuilder(jsonPath(ASSOCIATED_ARTIFACTS, ADMINSTRATIVE_AGREEMENT), false);
        //
        //        var queryToMatch = QueryBuilders.boolQuery()
        //            .must(publishedFileQuery)
        //            .must(notAdministrativeAgreementQuery);

        var include = new IncludeExclude(PUBLISHED_FILE, "");

        return AggregationBuilders
            .terms(ASSOCIATED_ARTIFACTS)
            .field(jsonPath(ASSOCIATED_ARTIFACTS, TYPE, KEYWORD))
            .includeExclude(include)
            .size(Defaults.DEFAULT_AGGREGATION_SIZE)
            .subAggregation(
                generateSimpleAggregation("public", jsonPath(ASSOCIATED_ARTIFACTS, ADMINSTRATIVE_AGREEMENT))

            );

        //        return new FilterAggregationBuilder(ASSOCIATED_ARTIFACTS, queryToMatch)
        //            .subAggregation(
        //                new NestedAggregationBuilder("associatedArtifact", jsonPath(ASSOCIATED_ARTIFACTS,
        //                "visibleForNonOwner", KEYWORD))
        //            );
    }
}
