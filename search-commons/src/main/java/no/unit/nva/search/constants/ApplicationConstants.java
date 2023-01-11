package no.unit.nva.search.constants;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import no.unit.nva.commons.json.JsonUtils;
import no.unit.nva.search.models.AggregationDto;
import nva.commons.core.Environment;

public final class ApplicationConstants {

    public static final String OPENSEARCH_ENDPOINT_INDEX = "resources";
    public static final String RESOURCES_INDEX = "resources";
    public static final String DOIREQUESTS_INDEX = "doirequests";
    public static final String MESSAGES_INDEX = "messages";
    public static final String PUBLISHING_REQUESTS_INDEX = "publishingrequests";

    public static final String TICKETS_INDEX = "tickets";

    public static final List<AggregationDto> AGGREGATIONS = List.of(
        new AggregationDto("entityDescription.reference.publicationInstance.type",
                           "entityDescription.reference.publicationInstance.type.keyword"),
        new AggregationDto("resourceOwner.owner",
                           "resourceOwner.owner.keyword"),
        new AggregationDto("resourceOwner.ownerAffiliation",
                           "resourceOwner.ownerAffiliation.keyword"),
        new AggregationDto("entityDescription.contributors.identity.name",
                           "entityDescription.contributors.identity.name.keyword"),
        new AggregationDto("entityDescription.contributors.affiliations.partOf.id.keyword",
                           "entityDescription.contributors.affiliations.partOf.id.keyword",
                           new AggregationDto(
                               "entityDescription.contributors.affiliations.id.keyword",
                               "entityDescription.contributors.affiliations.id.keyword"))
    );

    public static final List<String> TICKET_INDICES =
        List.of(DOIREQUESTS_INDEX, MESSAGES_INDEX, PUBLISHING_REQUESTS_INDEX);

    public static final List<String> ALL_INDICES = List.of(RESOURCES_INDEX, DOIREQUESTS_INDEX, MESSAGES_INDEX,
                                                           TICKETS_INDEX, PUBLISHING_REQUESTS_INDEX);

    public static final Environment ENVIRONMENT = new Environment();

    public static final String SEARCH_INFRASTRUCTURE_API_URI = readSearchInfrastructureApiUri();

    public static final String SEARCH_INFRASTRUCTURE_AUTH_URI = readSearchInfrastructureAuthUri();

    public static final ObjectMapper objectMapperWithEmpty = JsonUtils.dtoObjectMapper;
    public static final ObjectMapper objectMapperNoEmpty = JsonUtils.dynamoObjectMapper;

    private ApplicationConstants() {

    }

    private static String readSearchInfrastructureApiUri() {
        return ENVIRONMENT.readEnv("SEARCH_INFRASTRUCTURE_API_URI");
    }

    private static String readSearchInfrastructureAuthUri() {
        return ENVIRONMENT.readEnv("SEARCH_INFRASTRUCTURE_AUTH_URI");
    }
}
