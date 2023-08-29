package no.unit.nva.search2.constants;

import nva.commons.core.Environment;
import nva.commons.core.JacocoGenerated;

@JacocoGenerated
public final class ApplicationConstants {

    public static final Environment ENVIRONMENT = new Environment();
    public static final String HASH_KEY = "PrimaryKeyHashKey";
    public static final String SORT_KEY = "PrimaryKeyRangeKey";
    public static final String SECONDARY_INDEX_1_HASH_KEY = "SecondaryIndex1HashKey";
    public static final String SECONDARY_INDEX_1_RANGE_KEY = "SecondaryIndex1RangeKey";
    public static final String SECONDARY_INDEX_PUBLICATION_ID = "SearchByPublicationId";
    public static final String SEARCH_INFRASTRUCTURE_API_HOST = readSearchInfrastructureApiHost();
    public static final String SEARCH_INFRASTRUCTURE_AUTH_URI = readSearchInfrastructureAuthUri();

    private ApplicationConstants() {

    }

    private static String readSearchInfrastructureApiHost() {
        return ENVIRONMENT.readEnv("SEARCH_INFRASTRUCTURE_API_HOST");
    }

    private static String readSearchInfrastructureAuthUri() {
        return ENVIRONMENT.readEnv("SEARCH_INFRASTRUCTURE_AUTH_URI");
    }

    private static String readNviTableName() {
        return ENVIRONMENT.readEnv("NVI_TABLE_NAME");
    }

}
