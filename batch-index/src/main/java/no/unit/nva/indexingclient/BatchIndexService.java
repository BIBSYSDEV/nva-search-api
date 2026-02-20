package no.unit.nva.indexingclient;

import static nva.commons.core.attempt.Try.attempt;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.function.Predicate;
import java.util.stream.Stream;
import no.unit.nva.commons.json.JsonUtils;
import no.unit.nva.events.models.EventReference;
import no.unit.nva.indexingclient.models.IndexDocument;
import no.unit.nva.indexingclient.models.QueueClient;
import nva.commons.core.Environment;
import nva.commons.core.JacocoGenerated;
import nva.commons.core.attempt.Try;
import nva.commons.core.paths.UriWrapper;
import org.opensearch.action.bulk.BulkItemResponse;
import org.opensearch.action.bulk.BulkResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.amazon.awssdk.services.sqs.model.MessageAttributeValue;
import software.amazon.awssdk.services.sqs.model.SendMessageRequest;

public class BatchIndexService {

  private static final String READING_FROM_S3_FAILURE_MESSAGE =
      "Failed to fetch document from S3: {} {}";
  private static final String PARSING_EVENT_FAILURE_MESSAGE =
      "Failed to parse SQS message body: {} {}";
  private static final String BULK_INDEXING_FAILURE_MESSAGE = "Bulk indexing failed completely";
  private static final String INDEXING_SINGLE_DOCUMENT_FAILURE_MESSAGE =
      "Failed to index document: {} ";
  private static final String INDEXING_DOCUMENT_FAILURE_MESSAGE = "Document indexing failed %s %s";
  private static final String EXCEPTION_ATTRIBUTE = "exception";
  private static final String STRING_DATA_TYPE = "String";
  private static final String SENDING_TO_DLQ = "Sending message to DLQ: {}";
  private static final String INDEXING_SUMMARY_MESSAGE =
      "Batch indexing completed: {} total messages, {} documents indexed successfully, {} failed";
  private static final Logger LOGGER = LoggerFactory.getLogger(BatchIndexService.class);
  private static final String PERSISTED_RESOURCES_BUCKET = "PERSISTED_RESOURCES_BUCKET";
  private static final String DLQ_QUEUE = "DLQ_QUEUE";

  private final AsyncS3Driver asyncS3Driver;
  private final IndexingClient indexingClient;
  private final QueueClient queueClient;
  private final Environment environment;

  public BatchIndexService(
      AsyncS3Driver asyncS3Driver,
      IndexingClient indexingClient,
      QueueClient queueClient,
      Environment environment) {
    this.asyncS3Driver = asyncS3Driver;
    this.indexingClient = indexingClient;
    this.queueClient = queueClient;
    this.environment = environment;
  }

  @JacocoGenerated
  public static BatchIndexService defaultService() {
    var environment = new Environment();
    return new BatchIndexService(
        AsyncS3Driver.defaultDriver(),
        IndexingClient.defaultIndexingClient(),
        IndexQueueClient.defaultQueueClient(),
        environment);
  }

  public void batchIndex(Collection<String> messageBodies) {
    var results = processMessages(messageBodies);
    var documents = getDocumentsToIndex(results);
    var extractingDocumentsFailures = getFailures(results);

    var indexingResults = indexDocuments(documents, results);
    var indexingFailures = getFailures(indexingResults);

    logIndexingResult(messageBodies, extractingDocumentsFailures.size() + indexingFailures.size());

    Stream.of(extractingDocumentsFailures, indexingFailures)
        .flatMap(Collection::stream)
        .forEach(this::sendToDlq);
  }

  private List<SingleResult> processMessages(Collection<String> messageBodies) {
    var futures =
        messageBodies.stream().filter(distinctByUri()).map(this::processMessageAsync).toList();

    CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();

    return futures.stream().map(CompletableFuture::join).filter(Objects::nonNull).toList();
  }

  private CompletableFuture<SingleResult> processMessageAsync(String messageBody) {
    var eventReference = parseEventReference(messageBody);

    if (eventReference.isFailure()) {
      LOGGER.error(PARSING_EVENT_FAILURE_MESSAGE, messageBody, eventReference.getException());
      return CompletableFuture.completedFuture(
          SingleResult.failure(messageBody, eventReference.getException()));
    }

    return extractIndexDocument(messageBody, eventReference.get());
  }

  private CompletableFuture<SingleResult> extractIndexDocument(
      String messageBody, EventReference eventReference) {
    return asyncS3Driver
        .fetchAsync(environment.readEnv(PERSISTED_RESOURCES_BUCKET), getS3Key(eventReference))
        .thenApply(document -> SingleResult.success(messageBody, document))
        .exceptionally(
            ex -> {
              LOGGER.error(READING_FROM_S3_FAILURE_MESSAGE, eventReference.getUri(), ex);
              return SingleResult.failure(messageBody, (Exception) ex);
            });
  }

  private static String getS3Key(EventReference eventReference) {
    return UriWrapper.fromUri(eventReference.getUri()).toS3bucketPath().toString();
  }

  private Predicate<String> distinctByUri() {
    var seenUris = new HashSet<String>();
    return messageBody -> {
      var eventRef = parseEventReference(messageBody);
      return eventRef.isFailure() || seenUris.add(eventRef.get().getUri().toString());
    };
  }

  private Try<EventReference> parseEventReference(String messageBody) {
    return attempt(() -> JsonUtils.dtoObjectMapper.readValue(messageBody, EventReference.class));
  }

  private List<SingleResult> indexDocuments(
      Collection<IndexDocument> documents, Collection<SingleResult> results) {
    if (documents.isEmpty()) {
      return List.of();
    }

    var bulkResult = attempt(() -> indexingClient.batchInsert(documents.stream()).toList());

    if (bulkResult.isFailure()) {
      LOGGER.error(BULK_INDEXING_FAILURE_MESSAGE, bulkResult.getException());
      return documents.stream()
          .map(document -> findProcessResult(document, results))
          .map(result -> SingleResult.failure(result.messageBody(), bulkResult.getException()))
          .toList();
    }

    return createIndexingResults(bulkResult.get(), documents, results);
  }

  private List<SingleResult> createIndexingResults(
      Collection<BulkResponse> responses,
      Collection<IndexDocument> documents,
      Collection<SingleResult> results) {
    var failedDocumentsIdentifierList = getFailedDocumentsIdentifierList(responses);
    return documents.stream()
        .map(document -> createResultForDocument(document, failedDocumentsIdentifierList, results))
        .toList();
  }

  private SingleResult createResultForDocument(
      IndexDocument document,
      Collection<String> failedDocumentIdentifiers,
      Collection<SingleResult> processResults) {
    var result = findProcessResult(document, processResults);
    return failedDocumentIdentifiers.contains(document.getDocumentIdentifier())
        ? SingleResult.failure(result.messageBody(), indexingException(document))
        : result;
  }

  private SingleResult findProcessResult(IndexDocument document, Collection<SingleResult> results) {
    return results.stream()
        .filter(SingleResult::isSuccess)
        .filter(result -> result.document().equals(document))
        .findFirst()
        .orElseThrow();
  }

  private void sendToDlq(SingleResult failure) {
    LOGGER.info(SENDING_TO_DLQ, failure.messageBody());
    var request =
        SendMessageRequest.builder()
            .queueUrl(environment.readEnv(DLQ_QUEUE))
            .messageBody(failure.messageBody())
            .messageAttributes(Map.of(EXCEPTION_ATTRIBUTE, toMessageAttribute(failure.exception())))
            .build();

    queueClient.sendMessage(request);
  }

  private MessageAttributeValue toMessageAttribute(Exception exception) {
    return MessageAttributeValue.builder()
        .dataType(STRING_DATA_TYPE)
        .stringValue(exception.getMessage())
        .build();
  }

  private static void logIndexingResult(Collection<String> messageBodies, int indexingFailures) {
    var successfullyIndexed = messageBodies.size() - indexingFailures;
    LOGGER.info(
        INDEXING_SUMMARY_MESSAGE, messageBodies.size(), successfullyIndexed, indexingFailures);
  }

  private static List<SingleResult> getFailures(Collection<SingleResult> results) {
    return results.stream().filter(SingleResult::isFailure).toList();
  }

  private static List<IndexDocument> getDocumentsToIndex(Collection<SingleResult> results) {
    return results.stream().filter(SingleResult::isSuccess).map(SingleResult::document).toList();
  }

  private static RuntimeException indexingException(IndexDocument document) {
    var indexName = document.getIndexName();
    var documentIdentifier = document.getDocumentIdentifier();
    return new RuntimeException(
        INDEXING_DOCUMENT_FAILURE_MESSAGE.formatted(indexName, documentIdentifier));
  }

  private static List<String> getFailedDocumentsIdentifierList(Collection<BulkResponse> responses) {
    return responses.stream()
        .filter(BulkResponse::hasFailures)
        .map(BulkResponse::getItems)
        .flatMap(Arrays::stream)
        .filter(BulkItemResponse::isFailed)
        .peek(item -> LOGGER.error(INDEXING_SINGLE_DOCUMENT_FAILURE_MESSAGE, item.getId()))
        .map(BulkItemResponse::getId)
        .toList();
  }
}
