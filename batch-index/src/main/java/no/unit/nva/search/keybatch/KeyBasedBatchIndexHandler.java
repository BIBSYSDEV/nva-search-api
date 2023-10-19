package no.unit.nva.search.keybatch;

import static java.util.Objects.nonNull;
import static no.unit.nva.search.BatchIndexingConstants.defaultS3Client;
import static nva.commons.core.attempt.Try.attempt;
import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.S3Event;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Stream;
import no.unit.nva.s3.S3Driver;
import no.unit.nva.search.AggregationsValidator;
import no.unit.nva.search.IndexingClient;
import no.unit.nva.search.models.IndexDocument;
import nva.commons.core.Environment;
import nva.commons.core.JacocoGenerated;
import nva.commons.core.paths.UnixPath;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.amazon.awssdk.services.s3.S3Client;

public class KeyBasedBatchIndexHandler implements RequestHandler<S3Event, Void> {

    public static final String LINE_BREAK = "\n";
    public static final int SINGLE_RECORD = 0;
    public static final int MAX_PAYLOAD = 5_291_456;
    private static final Logger logger = LoggerFactory.getLogger(KeyBasedBatchIndexHandler.class);
    private static final String RESOURCES_BUCKET = new Environment().readEnv("PERSISTED_RESOURCES_BUCKET");
    public static final String LAST_CONSUMED_KEY_MESSAGE = "Last consumed key: {}";
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
        var content = fetchS3Content(bucket, key);
        var indexDocuments = mapToIndexDocuments(content);

        sendDocumentsToIndexInBatches(indexDocuments);

        logger.info(LAST_CONSUMED_KEY_MESSAGE, key);
        return null;
    }

    private static String getObjectKey(S3Event input) {
        return input.getRecords().get(SINGLE_RECORD).getS3().getObject().getKey();
    }

    private static String getBucketName(S3Event input) {
        return input.getRecords().get(SINGLE_RECORD).getS3().getBucket().getName();
    }

    private List<IndexDocument> mapToIndexDocuments(String content) {
        return extractIdentifiers(content).map(id -> fetchS3Content(RESOURCES_BUCKET, id))
                   .map(IndexDocument::fromJsonString)
                   .filter(this::isValid)
                   .toList();
    }

    private void sendDocumentsToIndexInBatches(List<IndexDocument> indexDocuments) {
        var documents = new ArrayList<IndexDocument>();
        var totalSize = 0;
        for (IndexDocument indexDocument : indexDocuments) {
            var currentFileSize = indexDocument.toJsonString().getBytes(StandardCharsets.UTF_8).length;
            if (totalSize + currentFileSize < MAX_PAYLOAD) {
                documents.add(indexDocument);
                totalSize += currentFileSize;
            } else {
                indexDocuments(documents);
                totalSize = 0;
                documents.clear();
                documents.add(indexDocument);
            }
        }
        if (!documents.isEmpty()) {
            indexDocuments(documents);
        }
    }

    private void indexDocuments(List<IndexDocument> list) {
        var response = attempt(() -> indexingClient.batchInsert(list.stream())).orElseThrow();
        if (nonNull(response)) {
            logger.info("Batch processed, has failures: {}", response.toList().get(0).hasFailures());
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
        var s3Driver = new S3Driver(s3Client, bucket);
        return attempt(() -> s3Driver.getFile(UnixPath.of(key))).orElseThrow();
    }
}
