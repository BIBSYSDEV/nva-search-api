package no.unit.nva.search;

import static nva.commons.core.attempt.Try.attempt;
import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.S3Event;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import no.unit.nva.search.models.IndexDocument;
import nva.commons.core.Environment;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;

public class BatchHandler implements RequestHandler<S3Event, Void> {

    private final String RESOURCES_BUCKET = new Environment().readEnv("EXPANDED_RESOURCES_BUCKET");
    private final IndexingClient indexingClient;
    private final S3Client s3Client;

    public BatchHandler(IndexingClient indexingClient, S3Client s3Client) {
        this.indexingClient = indexingClient;
        this.s3Client = s3Client;
    }

    @Override
    public Void handleRequest(S3Event input, Context context) {
        var bucketName = input.getRecords().get(0).getS3().getBucket().getName();
        var key = input.getRecords().get(0).getS3().getObject().getKey();
        var request = GetObjectRequest.builder().bucket(bucketName).key(key).build();
        var response = attempt(() -> s3Client.getObject(request).readAllBytes()).orElseThrow();
        var content = new String(response, StandardCharsets.UTF_8);
        var resources = Arrays.stream(content.split("\n"))
                            .map(identifier -> s3Client.getObject(GetObjectRequest.builder().bucket(RESOURCES_BUCKET).key(identifier).build()))
                            .map(res -> attempt(res::readAllBytes).orElseThrow())
                            .map(bytes -> new String(bytes, StandardCharsets.UTF_8))
                            .toList();
        var documentToIndex = resources.stream().map(IndexDocument::fromJsonString);
        indexingClient.batchInsert(documentToIndex);
        return null;
    }
}
