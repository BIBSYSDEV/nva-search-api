package no.unit.nva.search;

import static no.unit.nva.testutils.RandomDataGenerator.randomString;
import static nva.commons.core.attempt.Try.attempt;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import com.amazonaws.services.lambda.runtime.events.SQSEvent;
import com.amazonaws.services.lambda.runtime.events.SQSEvent.SQSMessage;
import java.net.URI;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import no.unit.nva.s3.S3Driver;
import no.unit.nva.stubs.FakeContext;
import no.unit.nva.stubs.FakeS3Client;
import nva.commons.core.JacocoGenerated;
import nva.commons.core.paths.UnixPath;
import nva.commons.core.paths.UriWrapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import software.amazon.awssdk.awscore.exception.AwsServiceException;
import software.amazon.awssdk.core.exception.SdkClientException;
import software.amazon.awssdk.services.s3.model.ListObjectsV2Request;
import software.amazon.awssdk.services.sqs.SqsClient;
import software.amazon.awssdk.services.sqs.model.GetQueueUrlRequest;
import software.amazon.awssdk.services.sqs.model.GetQueueUrlResponse;
import software.amazon.awssdk.services.sqs.model.SendMessageRequest;
import software.amazon.awssdk.services.sqs.model.SendMessageResponse;

class GenerateKeyBatchesHandlerTest {

    public static final String OUTPUT_BUCKET = "outputBucket";
    public static final String INPUT_BUCKET_PATH = "s3://inputBucket/resources";
    public static final String RESOURCES = "resources";
    public static final int SINGLE_FILE = 1;
    private S3Driver s3DriverInputBucket;
    private S3Driver s3DriverOutputBucket;
    private FakeS3Client outputClient;
    private FakeS3Client s3ClientInputClient;
    private FakeSqsClient sqsClient;
    private GenerateKeyBatchesHandler handler;

    @BeforeEach
    void setUp() {
        s3ClientInputClient = new FakeS3Client();
        s3DriverInputBucket = new S3Driver(s3ClientInputClient, INPUT_BUCKET_PATH);
        outputClient = new FakeS3Client();
        s3DriverOutputBucket = new S3Driver(outputClient, OUTPUT_BUCKET);
        sqsClient = new FakeSqsClient();
        handler = new GenerateKeyBatchesHandler(s3ClientInputClient, outputClient, INPUT_BUCKET_PATH,
                                                    OUTPUT_BUCKET, sqsClient);
    }

    @ParameterizedTest
    @ValueSource(ints = {1, 10, 25})
    void shouldReadS3KeysFromPersistedBucketAndWriteToS3BatchBucket(int numberOfItemsInBucket) {
        final var allFiles = putObjectsInInputBucket(numberOfItemsInBucket + 10);

        handler.handleRequest(createEventWithBody(numberOfItemsInBucket), new FakeContext());

        var actual = getPersistedFileFromOutputBucket();

        var expected = allFiles.subList(numberOfItemsInBucket, numberOfItemsInBucket + 10)
                           .stream().collect(Collectors.joining(System.lineSeparator()));

        assertThat(actual.size(), is(equalTo(SINGLE_FILE)));
        assertThat(actual.stream().collect(Collectors.joining(System.lineSeparator())), is(equalTo(expected)));
    }

    @Test
    void shouldReadS3KeysFromPersistedBucketAndWriteToS3BatchBucketWhenInputEventIsNull() {
        final var allFiles = putObjectsInInputBucket(20);

        handler.handleRequest(null, new FakeContext());

        var actual = getPersistedFileFromOutputBucket();
        var expected = allFiles.subList(0, 10)
                           .stream().collect(Collectors.joining(System.lineSeparator()));

        assertThat(actual.size(), is(equalTo(1)));
        assertThat(actual.stream().collect(Collectors.joining(System.lineSeparator())), is(equalTo(expected)));
    }

    @Test
    void shouldNotEmitNewEventWhenAllS3ObjectsHasBeenProcessed() {
        putObjectsInInputBucket(5);

        handler.handleRequest(null, new FakeContext());

        assertThat(sqsClient.getSentMessages(), hasSize(0));
    }

    @Test
    void shouldEmitNewEventWhenAllS3ObjectsHasBeenProcessed() {
        putObjectsInInputBucket(20);

        handler.handleRequest(null, new FakeContext());

        assertThat(sqsClient.getSentMessages(), hasSize(1));
    }

    @Test
    void shouldTriggerItself() {
        putObjectsInInputBucket(100);
        handler.handleRequest(null, new FakeContext());
        while(!sqsClient.getSentMessages().isEmpty()) {
            handler.handleRequest(createEventWithBody(
                , new FakeContext());
        }

    }


    @Test
    void shouldSplit() {
        putObjectsInInputBucket(20);

        handler.handleRequest(null, new FakeContext());

        assertThat(sqsClient.getSentMessages(), hasSize(1));
    }

    private SQSEvent createEventWithBody(int continuationToken) {
        var items = s3DriverInputBucket.listFiles(UnixPath.of(RESOURCES), null, 1000);
        var sqsEvent = new SQSEvent();
        var message = new SQSMessage();
        message.setBody(new KeyBatchMessage(String.valueOf(items.getFiles().get(continuationToken - 1))).toString());
        sqsEvent.setRecords(List.of(message));
        return sqsEvent;
    }

    private static String getBucketPath(UriWrapper uri) {
        return Path.of(UnixPath.fromString(uri.toString()).getPathElementByIndexFromEnd(1), uri.getLastPathElement())
                   .toString();
    }

    private List<String> getPersistedFileFromOutputBucket() {
        var request = ListObjectsV2Request.builder().bucket(OUTPUT_BUCKET).maxKeys(10_000).build();
        var content = outputClient.listObjectsV2(request).contents();
        return content.stream().map(object -> s3DriverOutputBucket.getFile(UnixPath.of(object.key()))).toList();
    }

    private List<String> putObjectsInInputBucket(int numberOfItems) {
        return IntStream.range(0, numberOfItems)
                   .mapToObj(item -> randomString())
                   .map(this::insertFileWithKey)
                   .map(UriWrapper::fromUri)
                   .map(GenerateKeyBatchesHandlerTest::getBucketPath)
                   .toList();
    }

    private URI insertFileWithKey(String key) {
        return attempt(
            () -> s3DriverInputBucket.insertFile(UnixPath.of(RESOURCES, key), randomString())).orElseThrow();
    }

    @JacocoGenerated
    public static class FakeSqsClient implements SqsClient {

        private final List<SendMessageRequest> sentMessages = new ArrayList<>();

        public List<SendMessageRequest> getSentMessages() {
            return sentMessages;
        }

        public GetQueueUrlResponse getQueueUrl(GetQueueUrlRequest getQueueUrlRequest)
            throws AwsServiceException, SdkClientException {
            return GetQueueUrlResponse.builder().queueUrl("").build();
        }

        public SendMessageResponse sendMessage(SendMessageRequest candidate)
            throws AwsServiceException, SdkClientException {
            sentMessages.add(candidate);
            return SendMessageResponse.builder()
                       .messageId(candidate.messageDeduplicationId())
                       .build();
        }

        public String serviceName() {
            return null;
        }

        public void close() {
            sentMessages.clear();
        }
    }
}