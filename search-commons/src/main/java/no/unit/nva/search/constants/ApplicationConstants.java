package no.unit.nva.search.constants;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import java.util.stream.Stream;
import no.unit.nva.commons.json.JsonUtils;
import nva.commons.core.Environment;
import org.opensearch.index.query.TermQueryBuilder;
import org.opensearch.search.aggregations.AbstractAggregationBuilder;
import org.opensearch.search.aggregations.AggregationBuilders;
import org.opensearch.search.aggregations.bucket.filter.FilterAggregationBuilder;
import org.opensearch.search.aggregations.bucket.nested.NestedAggregationBuilder;
import org.opensearch.search.aggregations.bucket.terms.TermsAggregationBuilder;

public final class ApplicationConstants {

    public static final String OPENSEARCH_ENDPOINT_INDEX = "resources";
    public static final String OPENSEARCH_TICKET_ENDPOINT_INDEX = "messages";
    public static final String RESOURCES_INDEX = "resources";
    public static final String IMPORT_CANDIDATES_INDEX = "import-candidates";
    public static final String DOIREQUESTS_INDEX = "doirequests";
    public static final String MESSAGES_INDEX = "messages";
    public static final String PUBLISHING_REQUESTS_INDEX = "publishingrequests";
    public static final String TICKETS_INDEX = "tickets";
    public static final String LABELS = "labels";
    public static final String KEYWORD = "keyword";
    public static final String ENGLISH_CODE = "en";
    public static final String BOKMAAL_CODE = "nb";
    public static final String NYNORSK_CODE = "nn";
    public static final String SAMI_CODE = "sme";
    public static final String ID = "id";
    public static final String JSON_PATH_DELIMITER = ".";
    public static final List<String> ALL_INDICES = List.of(RESOURCES_INDEX, DOIREQUESTS_INDEX, MESSAGES_INDEX,
                                                           TICKETS_INDEX, PUBLISHING_REQUESTS_INDEX);
    public static final Environment ENVIRONMENT = new Environment();
    public static final String SEARCH_INFRASTRUCTURE_API_URI = readSearchInfrastructureApiUri();
    public static final String SEARCH_INFRASTRUCTURE_AUTH_URI = readSearchInfrastructureAuthUri();
    public static final ObjectMapper objectMapperWithEmpty = JsonUtils.dtoObjectMapper;
    public static final ObjectMapper objectMapperNoEmpty = JsonUtils.dynamoObjectMapper;
    public static final int DEFAULT_AGGREGATION_SIZE = 100;
    public static final List<AbstractAggregationBuilder<? extends AbstractAggregationBuilder<?>>>
        IMPORT_CANDIDATES_AGGREGATIONS = List.of(
        generateSimpleAggregation("importStatus.candidateStatus",
                                  "importStatus.candidateStatus.keyword"),
        generateSimpleAggregation("publicationYear", "publicationYear.keyword"),
        generateObjectLabelsAggregation("organizations"),
        generateSimpleAggregation("collaborationType", "collaborationType.keyword"),
        generateImportedByUserAggregation()
    );

    public static final TermsAggregationBuilder TYPE_TERMS_AGGREGATION = generateSimpleAggregation("type",
                                                                                                   "type.keyword");
    public static final TermsAggregationBuilder STATUS_TERMS_AGGREGATION = generateSimpleAggregation("status",
                                                                                                     "status.keyword");
    public static final List<AbstractAggregationBuilder<? extends AbstractAggregationBuilder<?>>> TICKETS_AGGREGATIONS =
        List.of(TYPE_TERMS_AGGREGATION,
                STATUS_TERMS_AGGREGATION
        );
    public static final String ENTITY_DESCRIPTION = "entityDescription";
    public static final String CONTRIBUTORS = "contributors";
    public static final String SOURCE = "source";
    public static final String IDENTIFIER = "identifier";
    private static final String FUNDINGS = "fundings";
    public static final String NAME = "name";
    public static final String IDENTITY = "identity";
    public static final String REFERENCE = "reference";
    public static final String PUBLICATION_INSTANCE = "publicationInstance";
    public static final String TYPE = "type";
    public static final List<AbstractAggregationBuilder<? extends AbstractAggregationBuilder<?>>>
        RESOURCES_AGGREGATIONS = List.of(
        generateSimpleAggregation("resourceOwner.owner",
                                  "resourceOwner.owner.keyword"),
        generateSimpleAggregation("resourceOwner.ownerAffiliation",
                                  "resourceOwner.ownerAffiliation.keyword"),
        generateEntityDescriptionAggregation(),
        generateFundingSourceAggregation(),
        generateObjectLabelsAggregation("topLevelOrganization")
    );

    private ApplicationConstants() {

    }

    private static String readSearchInfrastructureApiUri() {
        return ENVIRONMENT.readEnv("SEARCH_INFRASTRUCTURE_API_URI");
    }

    private static String readSearchInfrastructureAuthUri() {
        return ENVIRONMENT.readEnv("SEARCH_INFRASTRUCTURE_AUTH_URI");
    }

    private static TermsAggregationBuilder generateSimpleAggregation(String term, String field) {
        return AggregationBuilders
                   .terms(term)
                   .field(field)
                   .size(DEFAULT_AGGREGATION_SIZE);
    }

    private static FilterAggregationBuilder generateImportedByUserAggregation() {
        return new FilterAggregationBuilder("importedByUser",
                                            new TermQueryBuilder("importStatus.candidateStatus.keyword", "IMPORTED"))
            .subAggregation(AggregationBuilders
                                .terms("importStatus.setBy")
                                .field("importStatus.setBy.keyword")
                                .size(DEFAULT_AGGREGATION_SIZE));
    }

    private static NestedAggregationBuilder generateTypeAggregation() {
        return new NestedAggregationBuilder(REFERENCE, jsonPath(ENTITY_DESCRIPTION, REFERENCE))
                   .subAggregation(generateNestedPublicationInstanceAggregation()
                                       .subAggregation(generatePublicationInstanceTypeAggregation()));
    }

    private static TermsAggregationBuilder generatePublicationInstanceTypeAggregation() {
        return generateSimpleAggregation(TYPE, jsonPath(ENTITY_DESCRIPTION, REFERENCE, PUBLICATION_INSTANCE, TYPE));
    }

    private static NestedAggregationBuilder generateNestedPublicationInstanceAggregation() {
        return new NestedAggregationBuilder(PUBLICATION_INSTANCE,
                                            jsonPath(ENTITY_DESCRIPTION, REFERENCE, PUBLICATION_INSTANCE));
    }

    private static NestedAggregationBuilder generateEntityDescriptionAggregation() {
        return new NestedAggregationBuilder(ENTITY_DESCRIPTION, ENTITY_DESCRIPTION)
                   .subAggregation(generateContributorAggregations())
                   .subAggregation(generateTypeAggregation());
    }

    private static NestedAggregationBuilder generateFundingSourceAggregation() {
        return
            new NestedAggregationBuilder(FUNDINGS, FUNDINGS)
                .subAggregation(
                    generateSimpleAggregation(IDENTIFIER, jsonPath(FUNDINGS, SOURCE, IDENTIFIER))
                        .subAggregation(
                            generateLabelsAggregation(jsonPath(FUNDINGS, SOURCE)))
            );
    }

    private static NestedAggregationBuilder generateContributorAggregations() {
        return
            generateNestedContributorAggregation()
                .subAggregation(
                    generateNestedIdentityAggregation()
                       .subAggregation(
                           generateIdAggregation()
                               .subAggregation(generateNameAggregation()))
                );
    }

    private static NestedAggregationBuilder generateNestedContributorAggregation() {
        return new NestedAggregationBuilder(CONTRIBUTORS, jsonPath(ENTITY_DESCRIPTION, CONTRIBUTORS));
    }

    private static NestedAggregationBuilder generateNestedIdentityAggregation() {
        return new NestedAggregationBuilder(IDENTITY, jsonPath(ENTITY_DESCRIPTION, CONTRIBUTORS, IDENTITY));
    }

    private static TermsAggregationBuilder generateIdAggregation() {
        return generateSimpleAggregation(ID, jsonPath(ENTITY_DESCRIPTION, CONTRIBUTORS, IDENTITY, ID));
    }

    private static TermsAggregationBuilder generateIdAggregation(String object) {
        return new TermsAggregationBuilder(ID)
                   .field(jsonPath(object, ID))
                   .size(DEFAULT_AGGREGATION_SIZE)
                   .subAggregation(generateLabelsAggregation(object));
    }

    private static TermsAggregationBuilder generateNameAggregation() {
        return generateSimpleAggregation(NAME, jsonPath(ENTITY_DESCRIPTION, CONTRIBUTORS, IDENTITY, NAME));
    }


    private static NestedAggregationBuilder generateObjectLabelsAggregation(String object) {
        return new NestedAggregationBuilder(object, object)
                   .subAggregation(generateIdAggregation(object));
    }

    private static NestedAggregationBuilder generateLabelsAggregation(String object) {
        var nestedAggregation = new NestedAggregationBuilder(LABELS, jsonPath(object, LABELS));
        Stream.of(BOKMAAL_CODE, ENGLISH_CODE, NYNORSK_CODE, SAMI_CODE)
            .map(code -> generateSimpleAggregation(code, jsonPath(object, LABELS, code, KEYWORD)))
            .forEach(nestedAggregation::subAggregation);
        return nestedAggregation;
    }

    private static String jsonPath(String... args) {
        return String.join(JSON_PATH_DELIMITER, args);
    }
}
