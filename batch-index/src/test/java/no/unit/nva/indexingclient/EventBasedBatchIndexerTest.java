package no.unit.nva.indexingclient;

import static no.unit.nva.LogAppender.getAppender;
import static no.unit.nva.LogAppender.logToString;
import static no.unit.nva.indexingclient.BatchIndexingConstants.NUMBER_OF_FILES_PER_EVENT_ENVIRONMENT_VARIABLE;
import static no.unit.nva.indexingclient.IndexingClient.objectMapper;
import static no.unit.nva.indexingclient.constants.ApplicationConstants.objectMapperWithEmpty;
import static no.unit.nva.testutils.RandomDataGenerator.randomJson;
import static no.unit.nva.testutils.RandomDataGenerator.randomString;

import static nva.commons.core.attempt.Try.attempt;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsEqual.equalTo;
import static org.hamcrest.core.IsIterableContaining.hasItem;
import static org.hamcrest.core.IsNot.not;
import static org.hamcrest.core.IsNull.nullValue;
import static org.hamcrest.core.StringContains.containsString;
import static org.junit.jupiter.api.Assertions.assertEquals;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;

import no.unit.nva.commons.json.JsonUtils;
import no.unit.nva.events.models.AwsEventBridgeEvent;
import no.unit.nva.identifiers.SortableIdentifier;
import no.unit.nva.indexing.testutils.FakeIndexingClient;
import no.unit.nva.indexingclient.models.EventConsumptionAttributes;
import no.unit.nva.indexingclient.models.IndexDocument;
import no.unit.nva.s3.S3Driver;
import no.unit.nva.stubs.FakeS3Client;

import nva.commons.core.attempt.Try;
import nva.commons.core.ioutils.IoUtils;
import nva.commons.core.paths.UnixPath;
import nva.commons.core.paths.UriWrapper;

import org.apache.logging.log4j.core.test.appender.ListAppender;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

@SuppressWarnings({"PMD.VariableDeclarationUsageDistance"})
public class EventBasedBatchIndexerTest extends BatchIndexTest {

    private static ListAppender appender;
    private EventBasedBatchIndexer indexer;
    private ByteArrayOutputStream outputStream;
    private FakeIndexingClient openSearchClient;
    private StubEventBridgeClient eventBridgeClient;
    private FakeS3Client s3Client;
    private S3Driver s3Driver;

    @BeforeAll
    public static void initClass() {
        appender = getAppender(BatchIndexer.class);
    }

    @BeforeEach
    public void init() {
        this.outputStream = new ByteArrayOutputStream();
        openSearchClient = mockOsClient();
        eventBridgeClient = new StubEventBridgeClient();
        s3Client = new FakeS3Client();
        s3Driver = new S3Driver(s3Client, "ingoredBucket");
        indexer =
                new EventBasedBatchIndexer(
                        s3Client,
                        openSearchClient,
                        eventBridgeClient,
                        NUMBER_OF_FILES_PER_EVENT_ENVIRONMENT_VARIABLE);
    }

    @ParameterizedTest(
            name =
                    "should return all ids for published resources that failed to be indexed. "
                            + "Input size:{0}")
    @ValueSource(ints = {1, 2, 5, 10, 100})
    public void shouldReturnsAllIdsForPublishedResourcesThatFailedToBeIndexed(
            int numberOfFilesPerEvent) throws JsonProcessingException {

        indexer =
                new EventBasedBatchIndexer(
                        s3Client,
                        failingOpenSearchClient(),
                        eventBridgeClient,
                        numberOfFilesPerEvent);
        var filesFailingToBeIndexed = randomFilesInSingleEvent(s3Driver, numberOfFilesPerEvent);
        var importLocation = filesFailingToBeIndexed.get(0).getHost().toString();
        var request = new ImportDataRequestEvent(importLocation);
        indexer.handleRequest(eventStream(request), outputStream, CONTEXT);

        var actualIdentifiersOfNonIndexedEntries =
                Arrays.stream(objectMapper.readValue(outputStream.toString(), String[].class))
                        .toList();
        var expectedIdentifiesOfNonIndexedEntries =
                Arrays.stream(extractIdentifiersFromFailingFiles(filesFailingToBeIndexed)).toList();

        assertEquals(actualIdentifiersOfNonIndexedEntries, expectedIdentifiesOfNonIndexedEntries);

        expectedIdentifiesOfNonIndexedEntries.forEach(
                expectedIdentifier ->
                        assertThat(logToString(appender), containsString(expectedIdentifier)));
    }

    @Test
    void batchIndexerParsesEvent() {
        InputStream event = IoUtils.inputStreamFromResources("event.json");
        indexer.handleRequest(event, outputStream, CONTEXT);
    }

    @ParameterizedTest(name = "batch indexer processes n files per request:{0}")
    @ValueSource(ints = {1, 2, 5, 10, 50, 100})
    void shouldIndexNFilesPerEvent(int numberOfFilesPerEvent) throws IOException {
        indexer =
                new EventBasedBatchIndexer(
                        s3Client, openSearchClient, eventBridgeClient, numberOfFilesPerEvent);
        var expectedFiles = randomFilesInSingleEvent(s3Driver, numberOfFilesPerEvent);
        var unexpectedFile = randomEntryInS3(s3Driver);

        var importLocation = unexpectedFile.getHost().getUri(); // all files are in the same bucket
        InputStream event = eventStream(new ImportDataRequestEvent(importLocation.toString()));
        indexer.handleRequest(event, outputStream, CONTEXT);

        for (var expectedFile : expectedFiles) {
            IndexDocument indexDocument = fetchIndexDocumentFromS3(expectedFile);
            assertThat(
                    openSearchClient.getIndex(indexDocument.getIndexName()),
                    hasItem(indexDocument.resource()));
        }

        IndexDocument notYetIndexedDocument = fetchIndexDocumentFromS3(unexpectedFile);
        assertThat(
                openSearchClient.getIndex(notYetIndexedDocument.getIndexName()),
                not(hasItem(notYetIndexedDocument.resource())));
    }

    @Test
    void shouldEmitEventForProcessingNextBatchWhenThereAreMoreFilesToProcess() throws IOException {
        var firstFile = randomEntryInS3(s3Driver);
        randomEntryInS3(s3Driver); // necessary second file for the emission of the next event

        String bucketUri = firstFile.getHost().getUri().toString();
        ImportDataRequestEvent firstEvent = new ImportDataRequestEvent(bucketUri);
        var event = eventStream(firstEvent);

        indexer.handleRequest(event, outputStream, CONTEXT);
        assertThat(
                eventBridgeClient.getLatestEvent().getStartMarker(),
                is(equalTo(firstFile.getLastPathElement())));
    }

    @Test
    void shouldNotEmitEventWhenThereAreNoMoreFilesToProcess() throws IOException {
        var firstFile = randomEntryInS3(s3Driver);
        randomEntryInS3(s3Driver);
        var bucketUri = firstFile.getHost().getUri().toString();
        var lastEvent = new ImportDataRequestEvent(bucketUri, firstFile.getLastPathElement());
        var event = eventStream(lastEvent);

        indexer.handleRequest(event, outputStream, CONTEXT);
        assertThat(eventBridgeClient.getLatestEvent(), is(nullValue()));
    }

    @Test
    void shouldIndexFirstFilesInFirstEventAndSubsequentFilesInNextEvent() throws IOException {
        var firstFile = randomEntryInS3(s3Driver);
        var secondFile = randomEntryInS3(s3Driver);
        var firstDocumentToIndex = fetchIndexDocumentFromS3(firstFile);
        var secondDocumentIndex = fetchIndexDocumentFromS3(secondFile);
        String bucketUri = firstFile.getHost().getUri().toString();

        var firstEvent = new ImportDataRequestEvent(bucketUri);
        indexer.handleRequest(eventStream(firstEvent), outputStream, CONTEXT);
        assertThatIndexHasFirstButNotSecondDocument(firstDocumentToIndex, secondDocumentIndex);

        var secondEvent = new ImportDataRequestEvent(bucketUri, firstFile.getLastPathElement());
        indexer.handleRequest(eventStream(secondEvent), outputStream, CONTEXT);
        assertThatIndexHasBothDocuments(firstDocumentToIndex, secondDocumentIndex);
    }

    private void assertThatIndexHasBothDocuments(
            IndexDocument firstDocumentToIndex, IndexDocument secondDocumentIndex) {
        assertThat(
                openSearchClient.getIndex(firstDocumentToIndex.getIndexName()),
                hasItem(firstDocumentToIndex.resource()));
        assertThat(
                openSearchClient.getIndex(secondDocumentIndex.getIndexName()),
                hasItem(secondDocumentIndex.resource()));
    }

    private void assertThatIndexHasFirstButNotSecondDocument(
            IndexDocument firstDocumentToIndex, IndexDocument secondDocumentIndex) {
        assertThat(
                openSearchClient.getIndex(firstDocumentToIndex.getIndexName()),
                hasItem(firstDocumentToIndex.resource()));
        assertThat(
                openSearchClient.getIndex(secondDocumentIndex.getIndexName()),
                not(hasItem(secondDocumentIndex.resource())));
    }

    private IndexDocument fetchIndexDocumentFromS3(UriWrapper expectedFile) {
        String indexDocumentJson = s3Driver.getFile(expectedFile.toS3bucketPath());
        return IndexDocument.fromJsonString(indexDocumentJson);
    }

    private String[] extractIdentifiersFromFailingFiles(List<UriWrapper> filesFailingToBeIndexed) {
        return filesFailingToBeIndexed.stream()
                .map(UriWrapper::getLastPathElement)
                .toList()
                .toArray(String[]::new);
    }

    private List<UriWrapper> randomFilesInSingleEvent(
            S3Driver s3Driver, int numberOfFilesPerEvent) {
        return IntStream.range(0, numberOfFilesPerEvent)
                .boxed()
                .map(attempt(ignored -> randomEntryInS3(s3Driver)))
                .map(Try::orElseThrow)
                .collect(Collectors.toList());
    }

    private UriWrapper randomEntryInS3(S3Driver s3Driver) throws IOException {
        var randomIndexDocument = randomIndexDocument();
        var filePath = UnixPath.of(randomIndexDocument.getDocumentIdentifier());
        return UriWrapper.fromUri(
                s3Driver.insertFile(filePath, randomIndexDocument.toJsonString()));
    }

    private IndexDocument randomIndexDocument() {
        return new IndexDocument(randomEventConsumptionAttributes(), randomObject());
    }

    private JsonNode randomObject() {
        var json = randomJson();
        return attempt(() -> JsonUtils.dtoObjectMapper.readTree(json)).orElseThrow();
    }

    private EventConsumptionAttributes randomEventConsumptionAttributes() {
        return new EventConsumptionAttributes(randomString(), SortableIdentifier.next());
    }

    private FakeIndexingClient mockOsClient() {
        return new FakeIndexingClient();
    }

    private InputStream eventStream(ImportDataRequestEvent eventDetail)
            throws JsonProcessingException {
        AwsEventBridgeEvent<ImportDataRequestEvent> event = new AwsEventBridgeEvent<>();
        event.setDetail(eventDetail);
        String jsonString = objectMapperWithEmpty.writeValueAsString(event);
        return IoUtils.stringToStream(jsonString);
    }
}
