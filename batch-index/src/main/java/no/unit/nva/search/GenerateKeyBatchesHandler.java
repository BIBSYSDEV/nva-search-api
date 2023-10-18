package no.unit.nva.search;

import static java.util.Objects.nonNull;
import static no.unit.nva.search.BatchIndexingConstants.defaultS3Client;
import static no.unit.nva.search.BatchIndexingConstants.defaultSqsClient;
import static nva.commons.core.attempt.Try.attempt;
import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.SQSEvent;
import java.nio.charset.StandardCharsets;
import java.util.UUID;
import java.util.stream.Collectors;
import no.unit.nva.commons.json.JsonUtils;
import nva.commons.core.Environment;
import nva.commons.core.JacocoGenerated;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.ListObjectsV2Request;
import software.amazon.awssdk.services.s3.model.ListObjectsV2Response;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.S3Object;
import software.amazon.awssdk.services.sqs.SqsClient;
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
        var continuationToken = getContinuationToken(input);
        var response = inputClient.listObjectsV2(createRequest(continuationToken, inputBucketName));
        writeObject(toKeySet(response));
        if (response.isTruncated()) {
            sqsClient.sendMessage(constructMessage(response.continuationToken()));
        }
        logger.info(PERSISTED_MESSAGE);
        return null;
    }

    private static String getContinuationToken(SQSEvent input) {
        return notEmptyEvent(input) ? parseMessageBody(input).continuationToken() : DEFAULT_CONTINUATION_TOKEN;
    }

    private static boolean notEmptyEvent(SQSEvent input) {
        return nonNull(input) && nonNull(input.getRecords()) && nonNull(input.getRecords().get(0));
    }

    private static SendMessageRequest constructMessage(String continuationToken) {
        return SendMessageRequest.builder()
                   .messageBody(new KeyBatchMessage(continuationToken).toString())
                   .queueUrl(KEY_BATCHES_QUEUE)
                   .build();
    }

    private static KeyBatchMessage parseMessageBody(SQSEvent input) {
        return attempt(() -> JsonUtils.dtoObjectMapper.readValue(input.getRecords().get(0).getBody(),
                                                                 KeyBatchMessage.class)).orElseThrow();
    }

    private static ListObjectsV2Request createRequest(String continuationToken, String bucketName) {
        return ListObjectsV2Request.builder()
                   .bucket(bucketName)
                   .prefix(RESOURCES_FOLDER)
                   .delimiter(DELIMITER)
                   .continuationToken(continuationToken)
                   .maxKeys(MAX_KEYS)
                   .build();
    }

    private static String toKeySet(ListObjectsV2Response response) {
        return response.contents().stream().map(S3Object::key).collect(Collectors.joining(System.lineSeparator()));
    }

    private void writeObject(String object) {
        var request = PutObjectRequest.builder().bucket(outputBucketName).key(UUID.randomUUID().toString()).build();
        outputClient.putObject(request, RequestBody.fromBytes(object.getBytes(StandardCharsets.UTF_8)));
    }
}
