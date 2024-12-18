package no.unit.nva.indexingclient;

import static no.unit.nva.search.model.constant.Defaults.ENVIRONMENT;

import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;

import no.unit.nva.search.model.IndexingClient;

import nva.commons.core.JacocoGenerated;

import software.amazon.awssdk.http.urlconnection.UrlConnectionHttpClient;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.eventbridge.EventBridgeClient;
import software.amazon.awssdk.services.s3.S3Client;

public final class Constants {

    static final String BATCH_INDEX_EVENT_TOPIC = "SearchService.Index.Batch";
    public static final String MANDATORY_UNUSED_SUBTOPIC = "DETAIL.WITH.TOPIC";
    static final String S3_LOCATION_FIELD = "s3Location";

    static final Config config = ConfigFactory.load();
    static final String PERSISTED_RESOURCES_PATH = config.getString("batch.persistedResourcesPath");
    static final String BATCH_INDEX_EVENT_BUS_NAME = config.getString("batch.index.eventbusname");
    static final boolean RECURSION_ENABLED = config.getBoolean("batch.index.recursion");

    public static final String EVENT_BUS = ENVIRONMENT.readEnv("EVENT_BUS");
    public static final String TOPIC = ENVIRONMENT.readEnv("TOPIC");

    private static final String AWS_REGION_ENV_VARIABLE = "AWS_REGION";
    private static final int NUMBER_OF_FILES_PER_EVENT =
            config.getInt("batch.index.number_of_files_per_event");

    public static final int NUMBER_OF_FILES_PER_EVENT_ENVIRONMENT_VARIABLE =
            ENVIRONMENT
                    .readEnvOpt("NUMBER_OF_FILES_PER_EVENT")
                    .map(Integer::parseInt)
                    .orElse(NUMBER_OF_FILES_PER_EVENT);

    @JacocoGenerated
    public Constants() {}

    @JacocoGenerated
    public static EventBridgeClient defaultEventBridgeClient() {
        return EventBridgeClient.builder().httpClient(UrlConnectionHttpClient.create()).build();
    }

    @JacocoGenerated
    public static IndexingClient defaultEsClient() {
        return IndexingClient.defaultIndexingClient();
    }

    @JacocoGenerated
    public static S3Client defaultS3Client() {
        String awsRegion =
                ENVIRONMENT.readEnvOpt(AWS_REGION_ENV_VARIABLE).orElse(Region.EU_WEST_1.toString());
        return S3Client.builder()
                .region(Region.of(awsRegion))
                .httpClient(UrlConnectionHttpClient.builder().build())
                .build();
    }
}
