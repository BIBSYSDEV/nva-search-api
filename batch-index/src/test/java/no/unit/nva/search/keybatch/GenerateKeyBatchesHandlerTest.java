package no.unit.nva.search.keybatch;

import static no.unit.nva.search.constants.ApplicationConstants.objectMapperWithEmpty;
import static no.unit.nva.testutils.RandomDataGenerator.randomString;
import static nva.commons.core.attempt.Try.attempt;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.core.Is.is;
import static org.mockito.Mockito.mock;
import com.amazonaws.services.lambda.runtime.Context;
import com.fasterxml.jackson.core.JsonProcessingException;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.net.URI;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import no.unit.nva.events.models.AwsEventBridgeEvent;
import no.unit.nva.s3.S3Driver;
import no.unit.nva.stubs.FakeContext;
import no.unit.nva.stubs.FakeS3Client;
import nva.commons.core.SingletonCollector;
import nva.commons.core.ioutils.IoUtils;
import nva.commons.core.paths.UnixPath;
import nva.commons.core.paths.UriWrapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import software.amazon.awssdk.services.eventbridge.EventBridgeClient;
import software.amazon.awssdk.services.eventbridge.model.PutEventsRequest;
import software.amazon.awssdk.services.eventbridge.model.PutEventsRequestEntry;
import software.amazon.awssdk.services.eventbridge.model.PutEventsResponse;
import software.amazon.awssdk.services.s3.model.ListObjectsV2Request;

class GenerateKeyBatchesHandlerTest {

    public static final String OUTPUT_BUCKET = "outputBucket";
    public static final String RESOURCES = "resources";
    private ByteArrayOutputStream outputStream;
    private FakeS3Client outputClient;
    private FakeS3Client inputClient;
    private StubEventBridgeClient eventBridgeClient;
    private GenerateKeyBatchesHandler handler;
    private S3Driver s3DriverInputBucket;
    private S3Driver s3DriverOutputBucket;

    @Test
    void shouldPersistS3KeysToBatchBucket() throws JsonProcessingException {
        final var allFiles = putObjectsInInputBucket(10);

        handler.handleRequest(eventStream(null), outputStream, mock(Context.class));

        var actual = getPersistedFileFromOutputBucket();
        var expected = allFiles.stream().collect(Collectors.joining(System.lineSeparator()));

        assertThat(actual.stream().collect(Collectors.joining(System.lineSeparator())), is(equalTo(expected)));
    }

    @Test
    void shouldEmitNewEventWhenS3BucketHasNotBeenTruncated() throws JsonProcessingException {
        var s3Objects = putObjectsInInputBucket(1001);

        handler.handleRequest(eventStream(null), outputStream, mock(Context.class));

        var emittedEvent = eventBridgeClient.getLatestEvent();

        assertThat(emittedEvent.startMarker(), is(equalTo(s3Objects.get(s3Objects.size() - 2))));
    }

    @BeforeEach
    void setUp() {
        this.outputStream = new ByteArrayOutputStream();
        inputClient = new FakeS3Client();
        s3DriverInputBucket = new S3Driver(inputClient, "inputBucket");
        outputClient = new FakeS3Client();
        s3DriverOutputBucket = new S3Driver(outputClient, "outputBucket");
        eventBridgeClient = new StubEventBridgeClient();
        handler = new GenerateKeyBatchesHandler(inputClient, outputClient, eventBridgeClient);
    }

    private static String getBucketPath(UriWrapper uri) {
        return Path.of(UnixPath.fromString(uri.toString()).getPathElementByIndexFromEnd(1), uri.getLastPathElement())
                   .toString();
    }

    private InputStream eventStream(KeyBatchRequestEvent detail) throws JsonProcessingException {
        var event = new AwsEventBridgeEvent<KeyBatchRequestEvent>();
        event.setDetail(detail);
        var jsonString = objectMapperWithEmpty.writeValueAsString(event);
        return IoUtils.stringToStream(jsonString);
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
        return attempt(() -> s3DriverInputBucket.insertFile(UnixPath.of(RESOURCES, key), randomString())).orElseThrow();
    }

    public class StubEventBridgeClient implements EventBridgeClient {

        private KeyBatchRequestEvent latestEvent;

        public KeyBatchRequestEvent getLatestEvent() {
            return latestEvent;
        }

        public PutEventsResponse putEvents(PutEventsRequest putEventsRequest) {
            this.latestEvent = saveContainedEvent(putEventsRequest);
            return PutEventsResponse.builder().failedEntryCount(0).build();
        }

        @Override
        public String serviceName() {
            return null;
        }

        @Override
        public void close() {

        }

        private KeyBatchRequestEvent saveContainedEvent(PutEventsRequest putEventsRequest) {
            PutEventsRequestEntry eventEntry = putEventsRequest.entries()
                                                   .stream()
                                                   .collect(SingletonCollector.collect());
            return attempt(eventEntry::detail).map(
                jsonString -> objectMapperWithEmpty.readValue(jsonString, KeyBatchRequestEvent.class)).orElseThrow();
        }
    }
}