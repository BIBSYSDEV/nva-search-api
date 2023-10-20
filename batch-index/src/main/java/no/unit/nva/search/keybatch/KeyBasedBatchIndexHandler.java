package no.unit.nva.search.keybatch;

import static java.util.Objects.nonNull;
import static no.unit.nva.search.BatchIndexingConstants.defaultS3Client;
import static no.unit.nva.search.EmitEventUtils.MANDATORY_UNUSED_SUBTOPIC;
import static no.unit.nva.search.keybatch.StartKeyBasedBatchHandler.EVENT_BUS;
import static no.unit.nva.search.keybatch.StartKeyBasedBatchHandler.TOPIC;
import static nva.commons.core.attempt.Try.attempt;
import com.amazonaws.services.lambda.runtime.Context;
import java.net.HttpURLConnection;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Stream;
import no.unit.nva.events.handlers.EventHandler;
import no.unit.nva.events.models.AwsEventBridgeEvent;
import no.unit.nva.s3.S3Driver;
import no.unit.nva.search.AggregationsValidator;
import no.unit.nva.search.IndexingClient;
import no.unit.nva.search.models.IndexDocument;
import nva.commons.core.Environment;
import nva.commons.core.JacocoGenerated;
import nva.commons.core.SingletonCollector;
import nva.commons.core.paths.UnixPath;
import org.opensearch.action.bulk.BulkResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.amazon.awssdk.http.urlconnection.UrlConnectionHttpClient;
import software.amazon.awssdk.services.eventbridge.EventBridgeClient;
import software.amazon.awssdk.services.eventbridge.model.PutEventsRequest;
import software.amazon.awssdk.services.eventbridge.model.PutEventsRequestEntry;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.ListObjectsV2Request;
import software.amazon.awssdk.services.s3.model.ListObjectsV2Response;

public class KeyBasedBatchIndexHandler extends EventHandler<KeyBatchRequestEvent, Void> {

    public static final String LINE_BREAK = "\n";
    public static final int MAX_PAYLOAD = 3_291_456;
    public static final String LAST_CONSUMED_BATCH = "Last consumed batch: {}";
    private static final Logger logger = LoggerFactory.getLogger(KeyBasedBatchIndexHandler.class);
    private static final String RESOURCES_BUCKET = new Environment().readEnv("PERSISTED_RESOURCES_BUCKET");
    private static final String KEY_BATCHES_BUCKET = new Environment().readEnv("KEY_BATCHES_BUCKET");
    private final IndexingClient indexingClient;
    private final S3Client s3ResourcesClient;
    private final S3Client s3BatchesClient;
    private final EventBridgeClient eventBridgeClient;

    @JacocoGenerated
    public KeyBasedBatchIndexHandler() {
        this(IndexingClient.defaultIndexingClient(), defaultS3Client(), defaultS3Client(), defaultEventBridgeClient());
    }

    public KeyBasedBatchIndexHandler(IndexingClient indexingClient, S3Client s3ResourcesClient,
                                     S3Client s3BatchesClient, EventBridgeClient eventBridgeClient) {
        super(KeyBatchRequestEvent.class);
        this.indexingClient = indexingClient;
        this.s3ResourcesClient = s3ResourcesClient;
        this.s3BatchesClient = s3BatchesClient;
        this.eventBridgeClient = eventBridgeClient;
    }

    @JacocoGenerated
    public static EventBridgeClient defaultEventBridgeClient() {
        return EventBridgeClient.builder().httpClient(UrlConnectionHttpClient.create()).build();
    }

    @Override
    protected Void processInput(KeyBatchRequestEvent input, AwsEventBridgeEvent<KeyBatchRequestEvent> event,
                                Context context) {
        var startMarker = getStartMarker(input);
        var batchResponse = fetchSingleBatch(startMarker);
        var batchKey = batchResponse.contents().get(0).key();
        var content = extractContent(batchKey);
        var indexDocuments = mapToIndexDocuments(content);

        sendDocumentsToIndexInBatches(indexDocuments, batchKey);

        var lastEvaluatedKey = batchResponse.contents().get(0).key();
        if (batchResponse.isTruncated()) {
            sendEvent(constructRequestEntry(lastEvaluatedKey, context));
        }
        logger.info(LAST_CONSUMED_BATCH, batchResponse.contents().get(0));
        return null;
    }

    private static PutEventsRequestEntry constructRequestEntry(String lastEvaluatedKey, Context context) {
        return PutEventsRequestEntry.builder()
                   .eventBusName(EVENT_BUS)
                   .detail(new KeyBatchRequestEvent(lastEvaluatedKey, TOPIC).toJsonString())
                   .detailType(MANDATORY_UNUSED_SUBTOPIC)
                   .source(KeyBasedBatchIndexHandler.class.getName())
                   .resources(context.getInvokedFunctionArn())
                   .time(Instant.now())
                   .build();
    }

    private static String getStartMarker(KeyBatchRequestEvent input) {

        return nonNull(input) && nonNull(input.getStartMarker()) ? input.getStartMarker() : null;
    }

    private static boolean isNotSuccess(Stream<BulkResponse> response) {
        return nonNull(response) && (!isSuccess(response) || !isCreated(response));
    }

    private static boolean isCreated(Stream<BulkResponse> response) {
        return response.collect(SingletonCollector.collect()).status().getStatus() != HttpURLConnection.HTTP_CREATED;
    }

    private static boolean isSuccess(Stream<BulkResponse> response) {
        return response.collect(SingletonCollector.collect()).status().getStatus() != HttpURLConnection.HTTP_OK;
    }

    private String extractContent(String key) {
        var s3Driver = new S3Driver(s3BatchesClient, KEY_BATCHES_BUCKET);
        return attempt(() -> s3Driver.getFile(UnixPath.of(key))).orElseThrow();
    }

    private void sendEvent(PutEventsRequestEntry event) {
        eventBridgeClient.putEvents(PutEventsRequest.builder().entries(event).build());
    }

    private ListObjectsV2Response fetchSingleBatch(String startMarker) {
        return s3BatchesClient.listObjectsV2(
            ListObjectsV2Request.builder().bucket(KEY_BATCHES_BUCKET).startAfter(startMarker).maxKeys(1).build());
    }

    private List<IndexDocument> mapToIndexDocuments(String content) {
        return extractIdentifiers(content).map(id -> fetchS3Content(RESOURCES_BUCKET, id))
                   .map(IndexDocument::fromJsonString)
                   .filter(this::isValid)
                   .toList();
    }

    private void sendDocumentsToIndexInBatches(List<IndexDocument> indexDocuments, String batchKey) {
        var documents = new ArrayList<IndexDocument>();
        var totalSize = 0;
        for (IndexDocument indexDocument : indexDocuments) {
            var currentFileSize = indexDocument.toJsonString().getBytes(StandardCharsets.UTF_8).length;
            if (totalSize + currentFileSize < MAX_PAYLOAD) {
                documents.add(indexDocument);
                totalSize += currentFileSize;
            } else {
                indexDocuments(documents, batchKey);
                totalSize = 0;
                documents.clear();
                documents.add(indexDocument);
            }
        }
        if (!documents.isEmpty()) {
            indexDocuments(documents, batchKey);
        }
    }

    private void indexDocuments(List<IndexDocument> list, String batchKey) {
        var response = attempt(() -> indexingClient.batchInsert(list.stream())).orElse(failure -> null);
        if (isNotSuccess(response)) {
            logger.error("Something went wrong, batch key: {}", batchKey);
        }
    }

    private boolean isValid(IndexDocument document) {
        var validator = new AggregationsValidator(document.getResource());
        if (!validator.isValid()) {
            logger.info(validator.getReport());
        }
        return validator.isValid();
    }

    private Stream<String> extractIdentifiers(String string) {
        return Arrays.stream(string.split(LINE_BREAK));
    }

    private String fetchS3Content(String bucket, String key) {
        var s3Driver = new S3Driver(s3ResourcesClient, bucket);
        return attempt(() -> s3Driver.getFile(UnixPath.of(key))).orElseThrow();
    }
}
