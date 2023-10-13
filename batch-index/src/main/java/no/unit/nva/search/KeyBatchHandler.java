package no.unit.nva.search;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestStreamHandler;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.UUID;
import java.util.stream.Collectors;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.ListObjectsV2Request;
import software.amazon.awssdk.services.s3.model.ListObjectsV2Response;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.S3Object;

public class KeyBatchHandler implements RequestStreamHandler {
    private final S3Client inputClient;
    private final S3Client outputClient;
    private final String inputBucketName;
    private final String outputBucketName;

    public KeyBatchHandler(S3Client inputClient, S3Client outputClient, String inputBucketName,
                           String outputBucketName) {
        this.inputClient = inputClient;
        this.outputClient = outputClient;
        this.inputBucketName = inputBucketName;
        this.outputBucketName = outputBucketName;
    }

    @Override
    public void handleRequest(InputStream input, OutputStream output, Context context) {
        var request = ListObjectsV2Request.builder().bucket(inputBucketName).maxKeys(10).build();
        inputClient.listObjectsV2Paginator(request).stream()
                       .map(KeyBatchHandler::toKeySet)
                       .forEach(this::writeObject);
    }

    private static String toKeySet(ListObjectsV2Response item) {
        return item.contents().stream().map(S3Object::key).collect(Collectors.joining(System.lineSeparator()));
    }

    private void writeObject(String object) {
        var request = PutObjectRequest.builder().bucket(outputBucketName).key(UUID.randomUUID().toString()).build();
        outputClient.putObject(request, RequestBody.fromBytes(object.getBytes(StandardCharsets.UTF_8)));
    }
}
