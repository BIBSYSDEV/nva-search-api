package no.unit.nva.indexingclient;

import static no.unit.nva.constants.Defaults.ENVIRONMENT;

import java.util.Optional;
import nva.commons.core.JacocoGenerated;
import software.amazon.awssdk.http.urlconnection.UrlConnectionHttpClient;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.eventbridge.EventBridgeClient;
import software.amazon.awssdk.services.s3.S3Client;

public final class Constants {

  public static final Optional<Boolean> RECURSION_ENABLED =
      ENVIRONMENT.readEnvOpt("BATCH_INDEX_ENABLE_RECURSION").map(Boolean::parseBoolean);
  public static final Optional<Integer> BATCH_SIZE =
      ENVIRONMENT.readEnvOpt("BATCH_SIZE").map(Integer::parseInt);
  public static final Optional<Integer> NUMBER_OF_FILES_PER_EVENT =
      ENVIRONMENT.readEnvOpt("NUMBER_OF_FILES_PER_EVENT").map(Integer::parseInt);
  public static final Optional<String> BATCH_INDEX_EVENT_BUS_NAME =
      ENVIRONMENT.readEnvOpt("BATCH_INDEX_EVENT_BUS_NAME");
  public static final Optional<String> EVENT_BUS = ENVIRONMENT.readEnvOpt("EVENT_BUS");
  public static final Optional<String> RESOURCES_BUCKET =
      ENVIRONMENT.readEnvOpt("PERSISTED_RESOURCES_BUCKET");
  public static final Optional<String> KEY_BATCHES_BUCKET =
      ENVIRONMENT.readEnvOpt("KEY_BATCHES_BUCKET");
  public static final Optional<String> PERSISTED_RESOURCES_PATH =
      ENVIRONMENT.readEnvOpt("PERSISTED_RESOURCES_PATH");
  public static final Optional<String> TOPIC = ENVIRONMENT.readEnvOpt("TOPIC");
  public static final String BATCH_INDEX_EVENT_TOPIC = "SearchService.Index.Batch";
  public static final String MANDATORY_UNUSED_SUBTOPIC = "DETAIL.WITH.TOPIC";
  public static final String S3_LOCATION_FIELD = "s3Location";
  private static final String AWS_REGION =
      ENVIRONMENT.readEnvOpt("AWS_REGION").orElse(Region.EU_WEST_1.toString());

  private Constants() {}

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
    return S3Client.builder()
        .region(Region.of(AWS_REGION))
        .httpClient(UrlConnectionHttpClient.builder().build())
        .build();
  }
}
