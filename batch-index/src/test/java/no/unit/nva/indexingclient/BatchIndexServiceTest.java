package no.unit.nva.indexingclient;

import static no.unit.nva.testutils.RandomDataGenerator.randomInteger;
import static no.unit.nva.testutils.RandomDataGenerator.randomString;
import static no.unit.nva.testutils.RandomDataGenerator.randomUri;
import static nva.commons.core.attempt.Try.attempt;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;
import no.unit.nva.commons.json.JsonUtils;
import no.unit.nva.events.models.EventReference;
import no.unit.nva.identifiers.SortableIdentifier;
import no.unit.nva.indexing.testutils.FakeIndexingClient;
import no.unit.nva.indexing.testutils.FakeSqsClient;
import no.unit.nva.indexingclient.models.EventConsumptionAttributes;
import no.unit.nva.indexingclient.models.IndexDocument;
import no.unit.nva.s3.S3Driver;
import no.unit.nva.stubs.FakeS3Client;
import nva.commons.core.paths.UnixPath;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.opensearch.action.DocWriteRequest.OpType;
import org.opensearch.action.bulk.BulkItemResponse;
import org.opensearch.action.bulk.BulkResponse;

class BatchIndexServiceTest {

  private BatchIndexService batchIndexService;
  private S3Driver s3Driver;
  private FakeSqsClient queueClient;
  private FakeIndexingClient indexingClient;

  @BeforeEach
  void setUp() {
    var s3Client = new FakeS3Client();
    this.s3Driver = new S3Driver(s3Client, randomString());
    this.queueClient = new FakeSqsClient();
    this.indexingClient = new FakeIndexingClient();
    batchIndexService = new BatchIndexService(s3Driver, indexingClient, queueClient);
  }

  @Test
  void shouldNotFailWhenMessageAreEmpty() {
    assertDoesNotThrow(() -> batchIndexService.batchIndex(List.of()));
  }

  @Test
  void shouldHandleNullEntriesInList() {
    var list = new ArrayList<String>();
    list.add(null);
    assertDoesNotThrow(() -> batchIndexService.batchIndex(list));
  }

  @Test
  void
      shouldPlaceMessageToDlqWithTheSameMessageBodyAsOriginalMessageWhenSqsMessageBodyIsNotParsable() {
    var messageBody = randomString();
    batchIndexService.batchIndex(List.of(messageBody));

    var messagePlacedOnDlq = queueClient.getDeliveredMessages().getFirst();

    assertEquals(messageBody, messagePlacedOnDlq.messageBody());
  }

  @Test
  void
      shouldPlaceMessageToDlqWithTheSameMessageBodyAsOriginalMessageWhenFailingOnFetchingDocumentFromS3() {
    var messageBody =
        new EventReference(randomString(), randomString(), randomUri()).toJsonString();
    batchIndexService.batchIndex(List.of(messageBody));

    var messagePlacedOnDlq = queueClient.getDeliveredMessages().getFirst();

    assertEquals(messageBody, messagePlacedOnDlq.messageBody());
  }

  @Test
  void
      shouldPlaceMessageToDlqWithTheSameMessageBodyAsOriginalMessageWhenIndexClientFailsOnIndexing()
          throws IOException {
    var document = randomIndexDocument();
    var documentS3Location =
        s3Driver.insertFile(UnixPath.of(randomString()), document.toJsonString());
    var messageBody =
        new EventReference(randomString(), randomString(), documentS3Location).toJsonString();
    var batchIndexService = batchServiceFailingWhenIndexing();
    batchIndexService.batchIndex(List.of(messageBody));

    var messagePlacedOnDlq = queueClient.getDeliveredMessages().getFirst();

    assertEquals(messageBody, messagePlacedOnDlq.messageBody());
  }

  @Test
  void
      shouldPlaceMessagesToDlqWithTheSameMessageBodyAsOriginalMessageWhenIndexingSucceededButDocumentFailed()
          throws IOException {
    var document = randomIndexDocument();
    var documentS3Location =
        s3Driver.insertFile(UnixPath.of(randomString()), document.toJsonString());
    var messageBody =
        new EventReference(randomString(), randomString(), documentS3Location).toJsonString();

    var service = batchIndexServiceReturningFailedDocument(document);

    service.batchIndex(List.of(messageBody));

    var messagePlacedOnDlq = queueClient.getDeliveredMessages().getFirst();

    assertEquals(messageBody, messagePlacedOnDlq.messageBody());
  }

  @Test
  void shouldIndexDocumentSuccessfully() throws IOException {
    var document = randomIndexDocument();
    var documentS3Location =
        s3Driver.insertFile(UnixPath.of(randomString()), document.toJsonString());
    var messageBody =
        new EventReference(randomString(), randomString(), documentS3Location).toJsonString();

    batchIndexService.batchIndex(List.of(messageBody));

    assertTrue(queueClient.getDeliveredMessages().isEmpty());

    var indexedDocument = indexingClient.getIndex(document.getIndexName()).iterator().next();

    assertEquals(indexedDocument, document.resource());
  }

  private static IndexDocument randomIndexDocument() {
    return new IndexDocument(
        new EventConsumptionAttributes(randomString(), SortableIdentifier.next()),
        attempt(() -> JsonUtils.dtoObjectMapper.readTree("{}")).orElseThrow());
  }

  private BatchIndexService batchIndexServiceReturningFailedDocument(IndexDocument document) {
    var indexingClient = mock(IndexingClient.class);
    when(indexingClient.batchInsert(any()))
        .thenReturn(Stream.of(bulkResponseWithFailedDocument(document)));
    return new BatchIndexService(s3Driver, indexingClient, queueClient);
  }

  private BulkResponse bulkResponseWithFailedDocument(IndexDocument document) {
    var failure =
        new BulkItemResponse.Failure(
            document.getIndexName(), document.getDocumentIdentifier(), new Exception());

    var bulkItemResponse =
        new BulkItemResponse[] {new BulkItemResponse(randomInteger(), OpType.INDEX, failure)};
    return new BulkResponse(bulkItemResponse, 100L);
  }

  private BatchIndexService batchServiceFailingWhenIndexing() {
    var indexingClient = mock(IndexingClient.class);
    when(indexingClient.batchInsert(any())).thenThrow(RuntimeException.class);
    return new BatchIndexService(s3Driver, indexingClient, queueClient);
  }
}
