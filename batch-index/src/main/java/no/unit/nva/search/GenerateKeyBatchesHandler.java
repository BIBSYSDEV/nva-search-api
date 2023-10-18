package no.unit.nva.search;

import static java.util.Objects.nonNull;
import static java.util.UUID.randomUUID;
import static no.unit.nva.search.BatchIndexingConstants.defaultS3Client;
import static no.unit.nva.search.BatchIndexingConstants.defaultSqsClient;
import static nva.commons.core.attempt.Try.attempt;
import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.SQSEvent;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.stream.Collectors;
import no.unit.nva.commons.json.JsonUtils;
import nva.commons.core.Environment;
import nva.commons.core.JacocoGenerated;
import nva.commons.core.attempt.Failure;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.ListObjectsV2Request;
import software.amazon.awssdk.services.s3.model.ListObjectsV2Response;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.S3Object;
import software.amazon.awssdk.services.sqs.SqsClient;
import software.amazon.awssdk.services.sqs.model.GetQueueUrlRequest;
import software.amazon.awssdk.services.sqs.model.SendMessageRequest;

public class GenerateKeyBatchesHandler implements RequestHandler<SQSEvent, Void> {

    public static final String RESOURCES_FOLDER = "resources/";
    public static final String DEFAULT_BATCH_SIZE = "10";
    public static final String PERSISTED_MESSAGE = "Batches have been persisted successfully";
    public static final String DELIMITER = "/";
    public static final String DEFAULT_CONTINUATION_TOKEN = null;
    private static final Logger logger = LoggerFactory.getLogger(GenerateKeyBatchesHandler.class);
    private static final Environment ENVIRONMENT = new Environment();
    public static final String KEY_BATCHES_QUEUE = ENVIRONMENT.readEnv("KEY_BATCHES_QUEUE_NAME");
    public static final int MAX_KEYS = Integer.parseInt(
        ENVIRONMENT.readEnvOpt("BATCH_SIZE").orElse(DEFAULT_BATCH_SIZE));
    private static final String KEY_BATCH_MESSAGE_GROUP = ENVIRONMENT.readEnvOpt("KEY_BATCHES_MESSAGE_GROUP")
                                                              .orElse("KEY_BATCHES_GROUP_ID");
    private final S3Client inputClient;
    private final S3Client outputClient;
    private final String inputBucketName;
    private final String outputBucketName;
    private final SqsClient sqsClient;

    @JacocoGenerated
    public GenerateKeyBatchesHandler() {
        this(defaultS3Client(), defaultS3Client(), ENVIRONMENT.readEnv("PERSISTED_RESOURCES_BUCKET"),
             ENVIRONMENT.readEnv("KEY_BATCHES_BUCKET"), defaultSqsClient());
    }

    public GenerateKeyBatchesHandler(S3Client inputClient, S3Client outputClient, String inputBucketName,
                                     String outputBucketName, SqsClient sqsClient) {
        this.inputClient = inputClient;
        this.outputClient = outputClient;
        this.inputBucketName = inputBucketName;
        this.outputBucketName = outputBucketName;
        this.sqsClient = sqsClient;
    }

    @Override
    public Void handleRequest(SQSEvent input, Context context) {
        return attempt(() -> processMessage(input)).orElse(this::logMessage);
    }

    private static String getContinuationToken(SQSEvent input) {
        return notEmptyEvent(input) ? parseMessageBody(input).lastEvaluatedKey() : DEFAULT_CONTINUATION_TOKEN;
    }

    private static boolean notEmptyEvent(SQSEvent event) {
        return nonNull(event) && nonNull(event.getRecords()) && nonNull(event.getRecords().get(0));
    }

    private static KeyBatchMessage parseMessageBody(SQSEvent input) {
        return attempt(() -> JsonUtils.dtoObjectMapper.readValue(input.getRecords().get(0).getBody(),
                                                                 KeyBatchMessage.class)).orElseThrow();
    }

    private static ListObjectsV2Request createRequest(String lastEvaluatedKey, String bucketName) {
        return ListObjectsV2Request.builder()
                   .bucket(bucketName)
                   .prefix(RESOURCES_FOLDER)
                   .delimiter(DELIMITER)
                   .startAfter(lastEvaluatedKey)
                   .maxKeys(MAX_KEYS)
                   .build();
    }

    private static String toKeyString(ListObjectsV2Response response) {
        return response.contents().stream().map(S3Object::key).collect(Collectors.joining(System.lineSeparator()));
    }

    private Void logMessage(Failure<Void> input) {
        logger.error("Could not proceed event {}", input.getException().getMessage());
        return null;
    }

    private Void processMessage(SQSEvent input) {
        var lastEvaluatedKey = getContinuationToken(input);
        logger.error("Continuation token from event {}", lastEvaluatedKey);
        var response = inputClient.listObjectsV2(createRequest(lastEvaluatedKey, inputBucketName));
        var keys = response.contents().stream().map(S3Object::key).toList();
        var string = toKeyString(response);
        writeObject(string);
        logger.error("S3 bucket has been truncated: {}", response.isTruncated());
        if (response.isTruncated()) {
            var message = constructMessage(getLastEvaluatedKey(keys));
            sqsClient.sendMessage(message);
        }
        logger.info(PERSISTED_MESSAGE);
        return null;
    }

    private static String getLastEvaluatedKey(List<String> keys) {
        return keys.get(keys.size() - 1);
    }

    private SendMessageRequest constructMessage(String lastEvaluatedKey) {
        var message = SendMessageRequest.builder()
                   .messageBody(new KeyBatchMessage(lastEvaluatedKey).toString())
                   .queueUrl(getQueueUrl())
                   .messageGroupId(KEY_BATCH_MESSAGE_GROUP)
                   .messageDeduplicationId(randomUUID().toString())
                   .build();
        logger.info("Message to send: {}", message.toString());
        return message;
    }

    private String getQueueUrl() {
        return sqsClient.getQueueUrl(GetQueueUrlRequest.builder().queueName(KEY_BATCHES_QUEUE).build()).queueUrl();
    }

    private void writeObject(String object) {
        var request = PutObjectRequest.builder().bucket(outputBucketName).key(randomUUID().toString()).build();
        outputClient.putObject(request, RequestBody.fromBytes(object.getBytes(StandardCharsets.UTF_8)));
    }
}
