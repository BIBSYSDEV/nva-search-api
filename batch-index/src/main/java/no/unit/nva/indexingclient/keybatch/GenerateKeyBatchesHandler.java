package no.unit.nva.indexingclient.keybatch;

import static no.unit.nva.indexingclient.BatchIndexingConstants.defaultS3Client;
import static no.unit.nva.indexingclient.EmitEventUtils.MANDATORY_UNUSED_SUBTOPIC;
import static no.unit.nva.indexingclient.keybatch.KeyBasedBatchIndexHandler.EVENT_BUS;
import static no.unit.nva.indexingclient.keybatch.KeyBasedBatchIndexHandler.TOPIC;

import static java.util.Objects.nonNull;
import static java.util.UUID.randomUUID;

import com.amazonaws.services.lambda.runtime.Context;

import no.unit.nva.events.handlers.EventHandler;
import no.unit.nva.events.models.AwsEventBridgeEvent;
import no.unit.nva.indexingclient.EventBasedBatchIndexer;

import nva.commons.core.Environment;
import nva.commons.core.JacocoGenerated;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.http.urlconnection.UrlConnectionHttpClient;
import software.amazon.awssdk.services.eventbridge.EventBridgeClient;
import software.amazon.awssdk.services.eventbridge.model.PutEventsRequest;
import software.amazon.awssdk.services.eventbridge.model.PutEventsRequestEntry;
import software.amazon.awssdk.services.eventbridge.model.PutEventsResponse;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.ListObjectsV2Request;
import software.amazon.awssdk.services.s3.model.ListObjectsV2Response;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.S3Object;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.List;
import java.util.stream.Collectors;

public class GenerateKeyBatchesHandler extends EventHandler<KeyBatchRequestEvent, Void> {

    public static final String DEFAULT_BATCH_SIZE = "1000";
    public static final String DELIMITER = "/";
    public static final String DEFAULT_START_MARKER = null;
    public static final String START_MARKER_MESSAGE = "Start marker: {}";
    private static final Logger logger = LoggerFactory.getLogger(GenerateKeyBatchesHandler.class);
    private static final Environment ENVIRONMENT = new Environment();
    public static final String INPUT_BUCKET = ENVIRONMENT.readEnv("PERSISTED_RESOURCES_BUCKET");
    public static final String OUTPUT_BUCKET = ENVIRONMENT.readEnv("KEY_BATCHES_BUCKET");
    public static final int MAX_KEYS =
            Integer.parseInt(ENVIRONMENT.readEnvOpt("BATCH_SIZE").orElse(DEFAULT_BATCH_SIZE));
    private static final String DEFAULT_LOCATION = "resources";
    private final S3Client inputClient;
    private final S3Client outputClient;
    private final EventBridgeClient eventBridgeClient;

    @JacocoGenerated
    public GenerateKeyBatchesHandler() {
        this(defaultS3Client(), defaultS3Client(), defaultEventBridgeClient());
    }

    public GenerateKeyBatchesHandler(
            S3Client inputClient, S3Client outputClient, EventBridgeClient eventBridgeClient) {
        super(KeyBatchRequestEvent.class);
        this.inputClient = inputClient;
        this.outputClient = outputClient;
        this.eventBridgeClient = eventBridgeClient;
    }

    private static boolean isNotEmptyEvent(KeyBatchRequestEvent event) {
        return nonNull(event) && nonNull(event.location());
    }

    private static PutEventsRequestEntry constructRequestEntry(
            String lastEvaluatedKey, Context context, String location) {
        return PutEventsRequestEntry.builder()
                .eventBusName(EVENT_BUS)
                .detail(new KeyBatchRequestEvent(lastEvaluatedKey, TOPIC, location).toJsonString())
                .detailType(MANDATORY_UNUSED_SUBTOPIC)
                .source(EventBasedBatchIndexer.class.getName())
                .resources(context.getInvokedFunctionArn())
                .time(Instant.now())
                .build();
    }

    private static String getStartMarker(KeyBatchRequestEvent input) {
        return notEmptyEvent(input) ? input.startMarker() : DEFAULT_START_MARKER;
    }

    private static boolean notEmptyEvent(KeyBatchRequestEvent event) {
        return nonNull(event) && nonNull(event.startMarker());
    }

    private static ListObjectsV2Request createRequest(String startMarker, String location) {
        return ListObjectsV2Request.builder()
                .bucket(INPUT_BUCKET)
                .prefix(location + DELIMITER)
                .delimiter(DELIMITER)
                .startAfter(startMarker)
                .maxKeys(MAX_KEYS)
                .build();
    }

    private static String toKeyString(List<String> values) {
        return values.stream().collect(Collectors.joining(System.lineSeparator()));
    }

    private static List<String> getKeys(ListObjectsV2Response response) {
        return response.contents().stream().map(S3Object::key).toList();
    }

    private static String getLastEvaluatedKey(List<String> keys) {
        return keys.get(keys.size() - 1);
    }

    @JacocoGenerated
    private static EventBridgeClient defaultEventBridgeClient() {
        return EventBridgeClient.builder()
                .httpClientBuilder(UrlConnectionHttpClient.builder())
                .build();
    }

    @Override
    protected Void processInput(
            KeyBatchRequestEvent input,
            AwsEventBridgeEvent<KeyBatchRequestEvent> event,
            Context context) {
        var startMarker = getStartMarker(input);
        var location = getLocation(input);
        logger.debug(START_MARKER_MESSAGE, startMarker);
        var response = inputClient.listObjectsV2(createRequest(startMarker, location));
        var keys = getKeys(response);
        writeObject(toKeyString(keys));
        var lastEvaluatedKey = getLastEvaluatedKey(keys);
        var eventsResponse = sendEvent(constructRequestEntry(lastEvaluatedKey, context, location));
        logger.debug(eventsResponse.toString());
        return null;
    }

    private String getLocation(KeyBatchRequestEvent event) {
        return isNotEmptyEvent(event) ? event.location() : DEFAULT_LOCATION;
    }

    private PutEventsResponse sendEvent(PutEventsRequestEntry event) {
        return eventBridgeClient.putEvents(PutEventsRequest.builder().entries(event).build());
    }

    private void writeObject(String object) {
        var request =
                PutObjectRequest.builder()
                        .bucket(OUTPUT_BUCKET)
                        .key(randomUUID().toString())
                        .build();
        outputClient.putObject(
                request, RequestBody.fromBytes(object.getBytes(StandardCharsets.UTF_8)));
    }
}
