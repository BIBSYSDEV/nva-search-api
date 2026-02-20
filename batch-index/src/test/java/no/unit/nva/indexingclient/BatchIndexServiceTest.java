package no.unit.nva.indexingclient;

import static no.unit.nva.testutils.RandomDataGenerator.randomInteger;
import static no.unit.nva.testutils.RandomDataGenerator.randomString;
import static nva.commons.core.attempt.Try.attempt;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Stream;
import java.util.zip.GZIPOutputStream;
import no.unit.nva.commons.json.JsonUtils;
import no.unit.nva.events.models.EventReference;
import no.unit.nva.identifiers.SortableIdentifier;
import no.unit.nva.indexing.testutils.FakeIndexingClient;
import no.unit.nva.indexing.testutils.FakeSqsClient;
import no.unit.nva.indexingclient.models.EventConsumptionAttributes;
import no.unit.nva.indexingclient.models.IndexDocument;
import nva.commons.core.Environment;
import nva.commons.logutils.LogUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.opensearch.action.DocWriteRequest.OpType;
import org.opensearch.action.bulk.BulkItemResponse;
import org.opensearch.action.bulk.BulkResponse;
import software.amazon.awssdk.core.ResponseBytes;
import software.amazon.awssdk.core.async.AsyncResponseTransformer;
import software.amazon.awssdk.services.s3.S3AsyncClient;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;

class BatchIndexServiceTest {

  private BatchIndexService batchIndexService;
  private FakeSqsClient queueClient;
  private FakeIndexingClient indexingClient;
  private Environment environment;
  private S3AsyncClient s3AsyncClient;

  @BeforeEach
  void setUp() {
    this.s3AsyncClient = mock(S3AsyncClient.class);
    this.queueClient = new FakeSqsClient();
    this.indexingClient = new FakeIndexingClient();
    this.environment = mock(Environment.class);

    batchIndexService =
        new BatchIndexService(
            new AsyncS3Driver(s3AsyncClient), indexingClient, queueClient, environment);
  }

  @Test
  void shouldNotFailWhenMessageListIsEmpty() {
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
    var messageBody = randomEventReferenceWithUri(createS3Uri());

    when(s3AsyncClient.getObject(any(GetObjectRequest.class), any(AsyncResponseTransformer.class)))
        .thenReturn(CompletableFuture.failedFuture(new RuntimeException("S3 error")));

    batchIndexService.batchIndex(List.of(messageBody));

    var messagePlacedOnDlq = queueClient.getDeliveredMessages().getFirst();
    assertEquals(messageBody, messagePlacedOnDlq.messageBody());
  }

  @Test
  void
      shouldPlaceMessageToDlqWithTheSameMessageBodyAsOriginalMessageWhenIndexClientFailsOnIndexing() {
    var document = randomIndexDocument();
    var messageBody = randomEventReferenceWithUri(createS3Uri());

    mockS3Response(document.toJsonString());

    var failingService = batchServiceFailingWhenIndexing();
    failingService.batchIndex(List.of(messageBody));

    var messagePlacedOnDlq = queueClient.getDeliveredMessages().getFirst();
    assertEquals(messageBody, messagePlacedOnDlq.messageBody());
  }

  @Test
  void
      shouldPlaceMessagesToDlqWithTheSameMessageBodyAsOriginalMessageWhenIndexingSucceededButSingleDocumentFailed() {
    var document = randomIndexDocument();
    var messageBody = randomEventReferenceWithUri(createS3Uri());

    mockS3Response(document.toJsonString());

    var partiallyFailingService = batchIndexServiceReturningFailedDocument(document);
    partiallyFailingService.batchIndex(List.of(messageBody));

    var messagePlacedOnDlq = queueClient.getDeliveredMessages().getFirst();
    assertEquals(messageBody, messagePlacedOnDlq.messageBody());
  }

  @Test
  void shouldIndexDocumentSuccessfully() {
    var document = randomIndexDocument();
    var messageBody = randomEventReferenceWithUri(createS3Uri());

    mockS3Response(document.toJsonString());

    batchIndexService.batchIndex(List.of(messageBody));

    var indexedDocument = indexingClient.getIndex(document.getIndexName()).iterator().next();
    assertEquals(indexedDocument, document.resource());
  }

  @Test
  void shouldOnlyFetchFromS3OnceForDuplicateUris() {
    var document = randomIndexDocument();
    var s3Uri = createS3Uri();

    mockS3Response(document.toJsonString());

    batchIndexService.batchIndex(
        List.of(
            randomEventReferenceWithUri(s3Uri),
            randomEventReferenceWithUri(s3Uri),
            randomEventReferenceWithUri(s3Uri)));

    verify(s3AsyncClient, times(1))
        .getObject(any(GetObjectRequest.class), any(AsyncResponseTransformer.class));
  }

  @Test
  void shouldLogIndexingResult() {
    var logAppender = LogUtils.getTestingAppender(BatchIndexService.class);
    var document = randomIndexDocument();
    var messageBody = randomEventReferenceWithUri(createS3Uri());

    mockS3Response(document.toJsonString());

    batchIndexService.batchIndex(List.of(messageBody));

    assertTrue(
        logAppender
            .getMessages()
            .contains(
                "Batch indexing completed: 1 total messages, 1 documents indexed successfully, 0"
                    + " failed"));
  }

  private void mockS3Response(String content) {
    var gzippedContent = gzipContent(content);

    var responseBytes =
        ResponseBytes.fromByteArray(GetObjectResponse.builder().build(), gzippedContent);

    when(s3AsyncClient.getObject(any(GetObjectRequest.class), any(AsyncResponseTransformer.class)))
        .thenReturn(CompletableFuture.completedFuture(responseBytes));
  }

  private byte[] gzipContent(String content) {
    try (var byteStream = new ByteArrayOutputStream();
        var gzipStream = new GZIPOutputStream(byteStream)) {
      gzipStream.write(content.getBytes(StandardCharsets.UTF_8));
      gzipStream.finish();
      return byteStream.toByteArray();
    } catch (IOException e) {
      throw new RuntimeException("Failed to gzip content", e);
    }
  }

  private static String randomEventReferenceWithUri(URI s3Uri) {
    return new EventReference(randomString(), randomString(), s3Uri).toJsonString();
  }

  private static URI createS3Uri() {
    return URI.create("s3://" + randomString() + "/" + randomString());
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

    return new BatchIndexService(
        new AsyncS3Driver(s3AsyncClient), indexingClient, queueClient, environment);
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

    return new BatchIndexService(
        new AsyncS3Driver(s3AsyncClient), indexingClient, queueClient, environment);
  }
}
