package no.unit.nva.indexingclient.constants;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import no.unit.nva.commons.json.JsonUtils;
import nva.commons.core.Environment;

public final class ApplicationConstants {

    public static final String ID = "id";
    public static final String KEYWORD = "keyword";
    public static final String TYPE = "type";

    public static final String DOIREQUESTS_INDEX = "doirequests";
    public static final String IMPORT_CANDIDATES_INDEX = "import-candidates";
    public static final String MESSAGES_INDEX = "messages";
    public static final String PUBLISHING_REQUESTS_INDEX = "publishingrequests";
    public static final String RESOURCES_INDEX = "resources";
    public static final String TICKETS_INDEX = "tickets";
    public static final String SHARD_ID = "0";

    public static final List<String> ALL_INDICES =
        List.of(RESOURCES_INDEX, DOIREQUESTS_INDEX, MESSAGES_INDEX, TICKETS_INDEX, PUBLISHING_REQUESTS_INDEX);

    public static final ObjectMapper objectMapperWithEmpty = JsonUtils.dtoObjectMapper;
    public static final ObjectMapper objectMapperNoEmpty = JsonUtils.dynamoObjectMapper;

    private static final Environment ENVIRONMENT = new Environment();
    public static final String SEARCH_INFRASTRUCTURE_API_URI = readSearchInfrastructureApiUri();
    public static final String SEARCH_INFRASTRUCTURE_AUTH_URI = readSearchInfrastructureAuthUri();


    private ApplicationConstants() {

    }

    private static String readSearchInfrastructureApiUri() {
        return ENVIRONMENT.readEnv("SEARCH_INFRASTRUCTURE_API_URI");
    }

    private static String readSearchInfrastructureAuthUri() {
        return ENVIRONMENT.readEnv("SEARCH_INFRASTRUCTURE_AUTH_URI");
    }

}
