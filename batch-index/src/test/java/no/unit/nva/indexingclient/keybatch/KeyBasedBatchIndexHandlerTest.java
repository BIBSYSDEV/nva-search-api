package no.unit.nva.indexingclient.keybatch;

import static java.util.UUID.randomUUID;
import static no.unit.nva.indexingclient.IndexingClient.objectMapper;
import static no.unit.nva.indexingclient.constants.ApplicationConstants.objectMapperWithEmpty;
import static no.unit.nva.indexingclient.keybatch.KeyBasedBatchIndexHandler.DEFAULT_INDEX;
import static no.unit.nva.testutils.RandomDataGenerator.randomJson;
import static no.unit.nva.testutils.RandomDataGenerator.randomString;
import static nva.commons.core.attempt.Try.attempt;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.emptyIterable;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.nullValue;
import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.events.S3Event;
import com.amazonaws.services.lambda.runtime.events.models.s3.S3EventNotification.S3BucketEntity;
import com.amazonaws.services.lambda.runtime.events.models.s3.S3EventNotification.S3Entity;
import com.amazonaws.services.lambda.runtime.events.models.s3.S3EventNotification.S3EventNotificationRecord;
import com.amazonaws.services.lambda.runtime.events.models.s3.S3EventNotification.S3ObjectEntity;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;
import no.unit.nva.events.models.AwsEventBridgeEvent;
import no.unit.nva.identifiers.SortableIdentifier;
import no.unit.nva.s3.S3Driver;
import no.unit.nva.indexingclient.IndexingClient;
import no.unit.nva.indexingclient.models.EventConsumptionAttributes;
import no.unit.nva.indexingclient.models.IndexDocument;
import no.unit.nva.stubs.FakeS3Client;
import nva.commons.core.SingletonCollector;
import nva.commons.core.StringUtils;
import nva.commons.core.ioutils.IoUtils;
import nva.commons.core.paths.UnixPath;
import nva.commons.logutils.LogUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mockito;
import org.opensearch.action.bulk.BulkResponse;
import software.amazon.awssdk.services.eventbridge.EventBridgeClient;
import software.amazon.awssdk.services.eventbridge.model.PutEventsRequest;
import software.amazon.awssdk.services.eventbridge.model.PutEventsRequestEntry;
import software.amazon.awssdk.services.eventbridge.model.PutEventsResponse;

class KeyBasedBatchIndexHandlerTest {

    public static final String IDENTIFIER = "__IDENTIFIER__";
    private static final String VALID_PUBLICATION = IoUtils.stringFromResources(Path.of("publication.json"));
    private static final String INVALID_PUBLICATION = IoUtils.stringFromResources(Path.of("invalid_publication.json"));
    public static final String DEFAULT_LOCATION = "resources";
    private ByteArrayOutputStream outputStream;
    private S3Driver s3ResourcesDriver;
    private FakeS3Client s3ResourcesClient;
    private S3Driver s3BatchesDriver;
    private FakeS3Client s3BatchesClient;
    private FakeOpenSearchClient openSearchClient;
    private EventBridgeClient eventBridgeClient;
    private KeyBasedBatchIndexHandler handler;

    @BeforeEach
    public void init() {
        outputStream = new ByteArrayOutputStream();
        s3ResourcesClient = new FakeS3Client();
        s3ResourcesDriver = new S3Driver(s3ResourcesClient, "resources");
        s3BatchesClient = new FakeS3Client();
        s3BatchesDriver = new S3Driver(s3BatchesClient, "batchesBucket");
        openSearchClient = new FakeOpenSearchClient();
        eventBridgeClient = new StubEventBridgeClient();
        handler = new KeyBasedBatchIndexHandler(openSearchClient, s3ResourcesClient, s3BatchesClient,
                                                eventBridgeClient);
    }

    @Test
    void shouldReturnIndexDocumentsWhenIndexingInBatches() throws IOException {
        var expectedDocuments = createExpectedDocuments(10);
        var batch = expectedDocuments.stream()
                        .map(IndexDocument::getDocumentIdentifier)
                        .collect(Collectors.joining(System.lineSeparator()));
        var batchKey = randomString();
        s3BatchesDriver.insertFile(UnixPath.of(batchKey), batch);

        handler.handleRequest(eventStream(null), outputStream, Mockito.mock(Context.class));

        var documentsFromIndex = openSearchClient.getIndexedDocuments();

        assertThat(documentsFromIndex, containsInAnyOrder(expectedDocuments.toArray()));
    }

    @Test
    void shouldSkipEmptyBatches() throws IOException {
        var batchKey = randomString();
        s3BatchesDriver.insertFile(UnixPath.of(batchKey), StringUtils.EMPTY_STRING);

        handler.handleRequest(eventStream(null), outputStream, Mockito.mock(Context.class));

        var documentsFromIndex = openSearchClient.getIndexedDocuments();

        assertThat(documentsFromIndex, is(emptyIterable()));
    }

    @Test
    void shouldNotEmitNewEventWhenNoMoreBatchesToRetrieve() throws IOException {
        var expectedDocuments = createExpectedDocuments(10);
        var batch = expectedDocuments.stream()
                        .map(IndexDocument::getDocumentIdentifier)
                        .collect(Collectors.joining(System.lineSeparator()));
        var batchKey = randomString();
        s3BatchesDriver.insertFile(UnixPath.of(batchKey), batch);

        handler.handleRequest(eventStream(null), outputStream, Mockito.mock(Context.class));

        var emittedEvent = ((StubEventBridgeClient) eventBridgeClient).getLatestEvent();
        assertThat(emittedEvent, is(nullValue()));
    }

    @Test
    void shouldEmitNewEventWhenThereAreMoreBatchesToIndex() throws IOException {
        var expectedDocuments = createExpectedDocuments(10);
        var batch = expectedDocuments.stream()
                        .map(IndexDocument::getDocumentIdentifier)
                        .collect(Collectors.joining(System.lineSeparator()));
        var batchKey = randomString();
        s3BatchesDriver.insertFile(UnixPath.of(batchKey), batch);
        var expectedStarMarkerFromEmittedEvent = randomString();
        s3BatchesDriver.insertFile(UnixPath.of(expectedStarMarkerFromEmittedEvent), batch);
        var list = new ArrayList<String>();
        list.add(null);
        list.add(batchKey);
        list.add(expectedStarMarkerFromEmittedEvent);

        for (String s : list) {
            handler.handleRequest(eventStream(s), outputStream, Mockito.mock(Context.class));

            var emittedEvent = ((StubEventBridgeClient) eventBridgeClient).getLatestEvent();

            assertThat(emittedEvent.getStartMarker(), is(equalTo(batchKey)));
            assertThat(emittedEvent.getLocation(), is(equalTo(DEFAULT_LOCATION)));
        }
    }

    @Test
    void shouldRemoveDocumentsThatDoesNotHaveEntityDescription() throws IOException {
        var expectedDocuments = createExpectedDocuments(9);
        var notExpectedDocument = createInvalidDocument(INVALID_PUBLICATION.replace("entityDescription", "something"));
        var documents = getDocuments(expectedDocuments, notExpectedDocument);
        var batch = expectedDocuments.stream()
                        .map(IndexDocument::getDocumentIdentifier)
                        .collect(Collectors.joining(System.lineSeparator()));
        var batchKey = randomString();
        s3BatchesDriver.insertFile(UnixPath.of(batchKey), batch);

        handler.handleRequest(eventStream(randomString()), outputStream, Mockito.mock(Context.class));

        var documentsFromIndex = openSearchClient.getIndexedDocuments();

        assertThat(documentsFromIndex, containsInAnyOrder(expectedDocuments.toArray()));
    }

    @Test
    void shouldRemoveDocumentsThatDoesNotHaveReference() throws IOException {
        var expectedDocuments = createExpectedDocuments(9);
        var notExpectedDocument = createInvalidDocument(INVALID_PUBLICATION.replace("reference", "something"));
        var documents = getDocuments(expectedDocuments, notExpectedDocument);
        var batch = expectedDocuments.stream()
                        .map(IndexDocument::getDocumentIdentifier)
                        .collect(Collectors.joining(System.lineSeparator()));
        var batchKey = randomString();
        s3BatchesDriver.insertFile(UnixPath.of(batchKey), batch);

        handler.handleRequest(eventStream(randomString()), outputStream, Mockito.mock(Context.class));

        var documentsFromIndex = openSearchClient.getIndexedDocuments();

        assertThat(documentsFromIndex, containsInAnyOrder(expectedDocuments.toArray()));
    }

    @ParameterizedTest(name = "Should remove documents that do not have expected field {0}")
    @ValueSource(strings = {"entityDescription", "reference", "publicationContext", "publicationInstance"})
    void shouldRemoveDocumentsThatDoesNotHaveExpectedField(String field) throws IOException {
        final var appender = LogUtils.getTestingAppenderForRootLogger();
        var expectedDocuments = createExpectedDocuments(9);
        var notExpectedDocument = createInvalidDocument(INVALID_PUBLICATION.replace(field, "something"));
        var documents = getDocuments(expectedDocuments, notExpectedDocument);
        var batchContent = new ArrayList<IndexDocument>();
        batchContent.add(notExpectedDocument);
        batchContent.addAll(expectedDocuments);
        var batch = batchContent.stream()
                        .map(IndexDocument::getDocumentIdentifier)
                        .collect(Collectors.joining(System.lineSeparator()));
        var batchKey = randomString();
        s3BatchesDriver.insertFile(UnixPath.of(batchKey), batch);

        handler.handleRequest(eventStream(randomString()), outputStream, Mockito.mock(Context.class));

        var documentsFromIndex = openSearchClient.getIndexedDocuments();

        assertThat(appender.getMessages(), containsString("has missing fields"));
        assertThat(appender.getMessages(), containsString(field));
        assertThat(documentsFromIndex, containsInAnyOrder(expectedDocuments.toArray()));
        assertThat(documentsFromIndex, not(hasItem(notExpectedDocument)));
    }

    private static ArrayList<IndexDocument> getDocuments(List<IndexDocument> expectedDocuments,
                                                         IndexDocument notExpectedDocument) {
        var documents = new ArrayList<>(expectedDocuments);
        documents.add(notExpectedDocument);
        return documents;
    }

    private static EventConsumptionAttributes randomConsumptionAttribute() {
        return new EventConsumptionAttributes(DEFAULT_INDEX, SortableIdentifier.next());
    }

    private static ObjectNode jsonNode() {
        return attempt(() -> (ObjectNode) objectMapper.readTree(randomJson())).orElseThrow();
    }

    private InputStream eventStream(String startMarker) throws JsonProcessingException {
        var event = new AwsEventBridgeEvent<KeyBatchRequestEvent>();
        event.setDetail(new KeyBatchRequestEvent(startMarker, randomString(), DEFAULT_LOCATION));
        event.setId(randomString());
        var jsonString = objectMapperWithEmpty.writeValueAsString(event);
        return IoUtils.stringToStream(jsonString);
    }

    private InputStream eventStreamWithLocation(String startMarker, String location) throws JsonProcessingException {
        var event = new AwsEventBridgeEvent<KeyBatchRequestEvent>();
        event.setDetail(new KeyBatchRequestEvent(startMarker, randomString(), location));
        event.setId(randomString());
        var jsonString = objectMapperWithEmpty.writeValueAsString(event);
        return IoUtils.stringToStream(jsonString);
    }

    private IndexDocument createInvalidDocument(String value) {
        return Stream.of(new IndexDocument(randomConsumptionAttribute(), nodeFromString(value)))
                   .map(this::insertResourceInPersistedResourcesBucket)
                   .collect(SingletonCollector.collect());
    }

    private List<IndexDocument> createExpectedDocuments(int numberOfDocuments) {
        return IntStream.range(0, numberOfDocuments)
                   .mapToObj(i -> new IndexDocument(randomConsumptionAttribute(), randomValidNode()))
                   .map(this::insertResourceInPersistedResourcesBucket)
                   .toList();
    }

    private JsonNode randomValidNode() {
        return attempt(
            () -> objectMapper.readTree(VALID_PUBLICATION.replace(IDENTIFIER, randomUUID().toString()))).orElseThrow();
    }

    private JsonNode nodeFromString(String value) {
        return attempt(() -> objectMapper.readTree(value)).orElseThrow();
    }

    private IndexDocument insertResourceInPersistedResourcesBucket(IndexDocument document) {
        attempt(() -> s3ResourcesDriver.insertFile(UnixPath.of(document.getDocumentIdentifier()),
                                                   document.toJsonString())).orElseThrow();
        return document;
    }

    private S3Event createS3Event(List<IndexDocument> documents) throws IOException {
        var objectKey = randomString();
        var eventNotification = new S3EventNotificationRecord(randomString(), randomString(), randomString(),
                                                              Instant.now().toString(), randomString(), null, null,
                                                              createS3Entity(objectKey), null);
        var fileContent = documents.stream()
                              .map(IndexDocument::getDocumentIdentifier)
                              .collect(Collectors.joining(System.lineSeparator()));
        s3ResourcesDriver.insertFile(UnixPath.of(objectKey), fileContent);
        return new S3Event(List.of(eventNotification));
    }

    private S3Entity createS3Entity(String expectedObjectKey) {
        var bucket = new S3BucketEntity(randomString(), null, randomString());
        var object = new S3ObjectEntity(expectedObjectKey, 100L, randomString(), randomString(), randomString());
        var schemaVersion = randomString();
        return new S3Entity(randomString(), bucket, object, schemaVersion);
    }

    private static class FakeOpenSearchClient extends IndexingClient {

        private final List<IndexDocument> documents;

        public FakeOpenSearchClient() {
            super(null, null);
            this.documents = new ArrayList<>();
        }

        @Override
        public Stream<BulkResponse> batchInsert(Stream<IndexDocument> contents) {
            List<IndexDocument> list = contents.toList();
            documents.addAll(list);
            return null;
        }

        public List<IndexDocument> getIndexedDocuments() {
            return documents;
        }
    }

    private static class StubEventBridgeClient implements EventBridgeClient {

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