package no.unit.nva.search.constants;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import java.util.stream.Stream;
import no.unit.nva.commons.json.JsonUtils;
import nva.commons.core.Environment;
import org.opensearch.index.query.QueryBuilders;
import org.opensearch.index.query.TermQueryBuilder;
import org.opensearch.search.aggregations.AbstractAggregationBuilder;
import org.opensearch.search.aggregations.AggregationBuilders;
import org.opensearch.search.aggregations.bucket.filter.FilterAggregationBuilder;
import org.opensearch.search.aggregations.bucket.nested.NestedAggregationBuilder;
import org.opensearch.search.aggregations.bucket.terms.TermsAggregationBuilder;

public final class ApplicationConstants {

    public static final String OPENSEARCH_ENDPOINT_INDEX = "resources";
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
        generateSimpleAggregation("importStatus.candidateStatus", "importStatus.candidateStatus.keyword"),
        generateSimpleAggregation("publicationYear", "publicationYear.keyword"),
        generateObjectLabelsAggregation("organizations"),
        generateHasFileAggregation(),
        generateSimpleAggregation("instanceType", "publicationInstance.type"),
        generateSimpleAggregation("collaborationType", "collaborationType.keyword"),
        generateImportedByUserAggregation()
    );


    public static final String TYPE = "type";
    public static final String ASSOCIATED_ARTIFACTS = "associatedArtifacts";
    public static final String ADMINSTRATIVE_AGREEMENT = "administrativeAgreement";
    public static final String PUBLISHED_FILE = "PublishedFile";

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
        return new FilterAggregationBuilder(
            "importedByUser", new TermQueryBuilder("importStatus.candidateStatus.keyword", "IMPORTED"))
            .subAggregation(AggregationBuilders
                                .terms("importStatus.setBy")
                                .field("importStatus.setBy.keyword")
                                .size(DEFAULT_AGGREGATION_SIZE));
    }


    private static TermsAggregationBuilder generateIdAggregation(String object) {
        return new TermsAggregationBuilder(ID)
            .field(jsonPath(object, ID, KEYWORD))
            .size(DEFAULT_AGGREGATION_SIZE)
            .subAggregation(generateLabelsAggregation(object));
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

    private static FilterAggregationBuilder generateHasFileAggregation() {
        var publishedFileQuery = new TermQueryBuilder(jsonPath(ASSOCIATED_ARTIFACTS, TYPE, KEYWORD), PUBLISHED_FILE);
        var notAdministrativeAgreementQuery =
            new TermQueryBuilder(jsonPath(ASSOCIATED_ARTIFACTS, ADMINSTRATIVE_AGREEMENT), false);

        var queryToMatch = QueryBuilders.boolQuery()
            .must(publishedFileQuery)
            .must(notAdministrativeAgreementQuery);
        return new FilterAggregationBuilder(ASSOCIATED_ARTIFACTS, queryToMatch);
    }
}
