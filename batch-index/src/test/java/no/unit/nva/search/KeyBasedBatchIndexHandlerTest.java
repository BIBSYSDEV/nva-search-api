package no.unit.nva.search;

import static no.unit.nva.search.IndexingClient.objectMapper;
import static no.unit.nva.testutils.RandomDataGenerator.randomJson;
import static no.unit.nva.testutils.RandomDataGenerator.randomString;
import static nva.commons.core.attempt.Try.attempt;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.events.S3Event;
import com.amazonaws.services.lambda.runtime.events.models.s3.S3EventNotification.S3BucketEntity;
import com.amazonaws.services.lambda.runtime.events.models.s3.S3EventNotification.S3Entity;
import com.amazonaws.services.lambda.runtime.events.models.s3.S3EventNotification.S3EventNotificationRecord;
import com.amazonaws.services.lambda.runtime.events.models.s3.S3EventNotification.S3ObjectEntity;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.io.IOException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;
import no.unit.nva.identifiers.SortableIdentifier;
import no.unit.nva.s3.S3Driver;
import no.unit.nva.search.models.EventConsumptionAttributes;
import no.unit.nva.search.models.IndexDocument;
import no.unit.nva.stubs.FakeS3Client;
import nva.commons.core.paths.UnixPath;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.opensearch.action.DocWriteRequest.OpType;
import org.opensearch.action.DocWriteResponse;
import org.opensearch.action.bulk.BulkItemResponse;
import org.opensearch.action.bulk.BulkItemResponse.Failure;
import org.opensearch.action.bulk.BulkResponse;

class KeyBasedBatchIndexHandlerTest {

    private S3Driver s3Driver;
    private FakeS3Client s3Client;
    private FakeOpenSearchClient openSearchClient;
    private KeyBasedBatchIndexHandler handler;

    @BeforeEach
    public void init() {
        s3Client = new FakeS3Client();
        s3Driver = new S3Driver(s3Client, "someBucket");
        openSearchClient = new FakeOpenSearchClient();
        handler = new KeyBasedBatchIndexHandler(openSearchClient, s3Client);
    }

    @Test
    void shouldReturnIndexDocumentsWhenIndexingInBatches() throws IOException {
        var expectedDocuments = createExpectedDocuments();

        handler.handleRequest(createS3Event(expectedDocuments), Mockito.mock(Context.class));

        var documentsFromIndex = openSearchClient.getIndexedDocuments();

        assertThat(documentsFromIndex, containsInAnyOrder(expectedDocuments.toArray()));
    }

    @Test
    void shouldSaveBatchDocumentsWhenSuccessfullyIndexed() throws IOException {
        var expectedDocuments = createExpectedDocuments();
        var s3Event = createS3Event(expectedDocuments);

        var expectedReport = expectedDocuments.stream()
                                 .map(IndexDocument::getDocumentIdentifier)
                                 .collect(Collectors.joining(System.lineSeparator()));
        var expectedReportKey = s3Event.getRecords().get(0).getS3().getObject().getKey();
        handler.handleRequest(s3Event, Mockito.mock(Context.class));
        var savedReport = s3Driver.getFile(UnixPath.of(expectedReportKey));

        assertThat(savedReport, is(equalTo(expectedReport)));
    }

    private static EventConsumptionAttributes randomConsumptionAttribute() {
        return new EventConsumptionAttributes(randomString(), SortableIdentifier.next());
    }

    private static ObjectNode jsonNode() {
        return attempt(() -> (ObjectNode) objectMapper.readTree(randomJson())).orElseThrow();
    }

    private List<IndexDocument> createExpectedDocuments() {
        return IntStream.range(0, 10)
                   .mapToObj(i -> new IndexDocument(randomConsumptionAttribute(), jsonNode()))
                   .map(this::insertResourceInPersistedResourcesBucket)
                   .toList();
    }

    private IndexDocument insertResourceInPersistedResourcesBucket(IndexDocument document) {
        attempt(() -> s3Driver.insertFile(UnixPath.of(document.getDocumentIdentifier()),
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
        s3Driver.insertFile(UnixPath.of(objectKey), fileContent);
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
}