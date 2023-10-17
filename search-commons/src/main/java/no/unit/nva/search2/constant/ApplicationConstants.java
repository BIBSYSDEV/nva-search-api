package no.unit.nva.search2.constant;

import com.fasterxml.jackson.databind.ObjectMapper;
import no.unit.nva.commons.json.JsonUtils;
import nva.commons.core.Environment;
import nva.commons.core.JacocoGenerated;

@JacocoGenerated
public final class ApplicationConstants {

    public static final String AMPERSAND = "&";
    public static final String ASTERISK = "*";
    public static final String ALL = "ALL";
    public static final String AND = " AND ";
    public static final String COLON = ":";
    public static final String COMMA = ",";
    public static final String EQUAL = "=";
    public static final String OR = " OR ";
    public static final String PLUS = "+";
    public static final String PREFIX = "(";
    public static final String QUOTE = "'";
    public static final String RESOURCES = "resources";
    public static final String SEARCH = "_search";
    public static final String SUFFIX = ")";
    public static final String UNDERSCORE = "_";
    public static final String SEARCH_INFRASTRUCTURE_CREDENTIALS = "SearchInfrastructureCredentials";


    public static final ObjectMapper objectMapperWithEmpty = JsonUtils.dtoObjectMapper;
    public static final Environment ENVIRONMENT = new Environment();

    public static String readSearchInfrastructureAuthUri() {
        return ENVIRONMENT.readEnv("SEARCH_INFRASTRUCTURE_AUTH_URI");
    }

    public static String readSearchInfrastructureApiUri() {
        return ENVIRONMENT.readEnv("SEARCH_INFRASTRUCTURE_API_URI");
    }

}
