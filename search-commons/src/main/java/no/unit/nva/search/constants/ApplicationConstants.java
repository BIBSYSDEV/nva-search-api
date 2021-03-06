package no.unit.nva.search.constants;

import nva.commons.core.Environment;

public final class ApplicationConstants {

    public static final String ELASTIC_SEARCH_SERVICE_NAME = "es";
    public static final String ELASTIC_SEARCH_INDEX_REFRESH_INTERVAL = "index.refresh_interval";
    public static final String DEFAULT_API_SCHEME = "https";
    public static Environment ENVIRONMENT = new Environment();
    public static final String ELASTICSEARCH_REGION = readElasticSearchRegion();
    public static final String ELASTICSEARCH_ENDPOINT_API_SCHEME = readElasticSearchEndpointApiScheme();
    public static final String ELASTICSEARCH_ENDPOINT_INDEX = readIndexName();
    public static final String ELASTICSEARCH_ENDPOINT_ADDRESS = readElasticSearchEndpointAddress();

    private ApplicationConstants() {

    }

    private static String readElasticSearchRegion() {
        return ENVIRONMENT.readEnv("ELASTICSEARCH_REGION");
    }

    private static String readElasticSearchEndpointApiScheme() {
        return ENVIRONMENT.readEnvOpt("ELASTICSEARCH_ENDPOINT_API_SCHEME").orElse(DEFAULT_API_SCHEME);
    }

    private static String readElasticSearchEndpointAddress() {
        return ENVIRONMENT.readEnv("ELASTICSEARCH_ENDPOINT_ADDRESS");
    }

    private static String readIndexName() {
        return ENVIRONMENT.readEnv("ELASTICSEARCH_ENDPOINT_INDEX");
    }
}
