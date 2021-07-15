package no.unit.nva.search.constants;

import nva.commons.core.Environment;

public final class ApplicationConstants {

    public static final String ELASTIC_SEARCH_SERVICE_NAME = "es";
    public static final String ELASTIC_SEARCH_INDEX_REFRESH_INTERVAL = "index.refresh_interval";
    public static final String DEFAULT_API_SCHEME = "https";
    private static final String DEFAULT_ELASTICSEARCH_REGION = "eu-west-1";
    private static final String DEFAULT_ELASTICSEARCH_ENDPOINT_INDEX = "resources";
    private static final String DEFAULT_ELASTICSEARCH_ENDPOINT_ADDRESS = "hhtps://localhost";
    public static Environment ENVIRONMENT = new Environment();
    public static final String ELASTICSEARCH_REGION = readElasticSearchRegion();
    public static final String ELASTICSEARCH_ENDPOINT_INDEX = readIndexName();
    public static final String ELASTICSEARCH_ENDPOINT_ADDRESS = readElasticSearchEndpointAddress();

    private ApplicationConstants() {
    }

    private static String readElasticSearchRegion() {
        return ENVIRONMENT.readEnvOpt("ELASTICSEARCH_REGION").orElse(DEFAULT_ELASTICSEARCH_REGION);
    }

    private static String readElasticSearchEndpointAddress() {
        return ENVIRONMENT.readEnvOpt("ELASTICSEARCH_ENDPOINT_ADDRESS")
                .orElse(DEFAULT_ELASTICSEARCH_ENDPOINT_ADDRESS);
    }

    private static String readIndexName() {
        return ENVIRONMENT.readEnvOpt("ELASTICSEARCH_ENDPOINT_INDEX")
                .orElse(DEFAULT_ELASTICSEARCH_ENDPOINT_INDEX);
    }
}
