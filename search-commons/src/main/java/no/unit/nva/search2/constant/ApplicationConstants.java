package no.unit.nva.search2.constant;

import com.fasterxml.jackson.databind.ObjectMapper;
import no.unit.nva.commons.json.JsonUtils;
import nva.commons.core.Environment;

public final class ApplicationConstants {

    public static final ObjectMapper objectMapperWithEmpty = JsonUtils.dtoObjectMapper;

    public static final String AND = "+AND+";
    public static final String SEARCH = "_search";
    public static final String OR = "+OR+";

    public static final String SEARCH_INFRASTRUCTURE_CREDENTIALS = "SearchInfrastructureCredentials";
    public static final Environment ENVIRONMENT = new Environment();

    public static String readSearchInfrastructureAuthUri() {
        return ENVIRONMENT.readEnv("SEARCH_INFRASTRUCTURE_AUTH_URI");
    }
}
