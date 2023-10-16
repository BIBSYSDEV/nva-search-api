package no.unit.nva.search;

import static no.unit.nva.search.BatchIndexingConstants.defaultS3Client;
import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestStreamHandler;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.UUID;
import java.util.stream.Collectors;
import nva.commons.core.Environment;
import nva.commons.core.JacocoGenerated;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.ListObjectsV2Request.Builder;
import software.amazon.awssdk.services.s3.model.ListObjectsV2Response;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.S3Object;

public class GenerateKeyBatchesHandler implements RequestStreamHandler {

    private static final Logger logger = LoggerFactory.getLogger(GenerateKeyBatchesHandler.class);
    public static final String RESOURCE_DELIMITER = "/resources";
    public static final String DEFAULT_BATCH_SIZE = "10";
    private static final Environment ENVIRONMENT = new Environment();
    public static final int MAX_KEYS = Integer.parseInt(
        ENVIRONMENT.readEnvOpt("BATCH_SIZE").orElse(DEFAULT_BATCH_SIZE));
    private final S3Client inputClient;
    private final S3Client outputClient;
    private final String inputBucketName;
    private final String outputBucketName;

    @JacocoGenerated
    public GenerateKeyBatchesHandler() {
        this(defaultS3Client(), defaultS3Client(), ENVIRONMENT.readEnv("PERSISTED_RESOURCES_BUCKET"),
             ENVIRONMENT.readEnv("KEY_BATCHES_BUCKET"));
    }

    public GenerateKeyBatchesHandler(S3Client inputClient, S3Client outputClient, String inputBucketName,
                                     String outputBucketName) {
        this.inputClient = inputClient;
        this.outputClient = outputClient;
        this.inputBucketName = inputBucketName;
        this.outputBucketName = outputBucketName;
    }

    @Override
    public void handleRequest(InputStream input, OutputStream output, Context context) {
        logger.info("Reading from bucket {}", inputBucketName);
        inputClient.listObjectsV2Paginator(request -> requestBuilder(request, inputBucketName))
            .stream()
            .map(GenerateKeyBatchesHandler::toKeySet)
            .forEach(this::writeObject);
    }

    private static void requestBuilder(Builder request, String bucketName) {
        request.bucket(bucketName).delimiter(RESOURCE_DELIMITER).maxKeys(MAX_KEYS);
    }

    private static String toKeySet(ListObjectsV2Response item) {
        return item.contents().stream().map(S3Object::key).collect(Collectors.joining(System.lineSeparator()));
    }

    private void writeObject(String object) {
        var request = PutObjectRequest.builder().bucket(outputBucketName).key(UUID.randomUUID().toString()).build();
        outputClient.putObject(request, RequestBody.fromBytes(object.getBytes(StandardCharsets.UTF_8)));
    }
}
