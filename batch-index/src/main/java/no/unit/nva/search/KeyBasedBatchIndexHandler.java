package no.unit.nva.search;

import static no.unit.nva.search.BatchIndexingConstants.defaultS3Client;
import static nva.commons.core.attempt.Try.attempt;
import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.S3Event;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.stream.Stream;
import no.unit.nva.search.models.IndexDocument;
import nva.commons.core.Environment;
import nva.commons.core.JacocoGenerated;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;

public class KeyBasedBatchIndexHandler implements RequestHandler<S3Event, Void> {

    public static final String LINE_BREAK = "\n";
    public static final int SINGLE_RECORD = 0;
    private static final String RESOURCES_BUCKET = new Environment().readEnv("PERSISTED_RESOURCES_BUCKET");
    private final IndexingClient indexingClient;
    private final S3Client s3Client;

    @JacocoGenerated
    public KeyBasedBatchIndexHandler() {
        this(IndexingClient.defaultIndexingClient(), defaultS3Client());
    }

    public KeyBasedBatchIndexHandler(IndexingClient indexingClient, S3Client s3Client) {
        this.indexingClient = indexingClient;
        this.s3Client = s3Client;
    }

    @Override
    public Void handleRequest(S3Event input, Context context) {
        var bucket = getBucketName(input);
        var key = getObjectKey(input);
        var request = createRequest(bucket, key);
        var content = fetchS3Content(request);

        var resourcesToIndex = extractIdentifiers(content)
                                   .map(id -> createRequest(RESOURCES_BUCKET, id))
                                   .map(this::fetchS3Content)
                                   .map(IndexDocument::fromJsonString);

        indexingClient.batchInsert(resourcesToIndex);
        return null;
    }

    private static String toString(byte[] response) {
        return new String(response, StandardCharsets.UTF_8);
    }

    private static GetObjectRequest createRequest(String bucketName, String key) {
        return GetObjectRequest.builder().bucket(bucketName).key(key).build();
    }

    private static String getObjectKey(S3Event input) {
        return input.getRecords().get(SINGLE_RECORD).getS3().getObject().getKey();
    }

    private static String getBucketName(S3Event input) {
        return input.getRecords().get(SINGLE_RECORD).getS3().getBucket().getName();
    }

    private Stream<String> extractIdentifiers(String string) {
        return Arrays.stream(string.split(LINE_BREAK));
    }

    private String fetchS3Content(GetObjectRequest request) {
        return attempt(() -> s3Client.getObject(request)).map(InputStream::readAllBytes)
                   .map(KeyBasedBatchIndexHandler::toString)
                   .orElseThrow();
    }
}
