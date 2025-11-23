package no.unit.nva.indexingclient;

import static no.unit.nva.constants.Defaults.ENVIRONMENT;
import static nva.commons.core.attempt.Try.attempt;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Stream;
import no.unit.nva.commons.json.JsonUtils;
import no.unit.nva.events.models.EventReference;
import no.unit.nva.indexingclient.models.IndexDocument;
import no.unit.nva.indexingclient.models.QueueClient;
import no.unit.nva.s3.S3Driver;
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

  public static final String FAILED_DOCUMENT_MESSAGE = "Failed to index document: {} at {}";
  public static final String FAILED_TO_FETCH_FROM_S3_MESSAGE =
      "Failed to fetch IndexDocument from S3 for event " + "reference: {}";
  public static final String FAILED_TO_PARSE_SQS_MESSAGE = "Failed to parse SQS message: {} {}";
  public static final String DLQ_QUEUE = "DLQ_QUEUE";
  private static final Logger LOGGER = LoggerFactory.getLogger(BatchIndexService.class);
  private static final String PERSISTED_RESOURCES_BUCKET =
      ENVIRONMENT.readEnv("PERSISTED_RESOURCES_BUCKET");

  private final S3Driver s3Driver;
  private final IndexingClient indexingClient;
  private final QueueClient queueClient;
  private final Map<String, String> documentIdToMessageMap;
  private final Map<String, Exception> failedEntries;

  public BatchIndexService(
      S3Driver s3Driver, IndexingClient indexingClient, QueueClient queueClient) {
    this.s3Driver = s3Driver;
    this.indexingClient = indexingClient;
    this.queueClient = queueClient;
    this.failedEntries = new ConcurrentHashMap<>();
    this.documentIdToMessageMap = new ConcurrentHashMap<>();
  }

  @JacocoGenerated
  public static BatchIndexService defaultService() {
    return new BatchIndexService(
        new S3Driver(S3Driver.defaultS3Client().build(), PERSISTED_RESOURCES_BUCKET),
        IndexingClient.defaultIndexingClient(),
        IndexQueueClient.defaultQueueClient());
  }

  public void batchIndex(Collection<String> messageBodies) {
    var messages = messageBodies.stream().filter(Objects::nonNull).toList();
    var documents = fetchDocuments(messages);
    var response = attempt(() -> indexingClient.batchInsert(documents.stream()));

    if (response.isFailure()) {
      addAllDocumentsToFailedEntries(documents, response);
    } else {
      for (BulkItemResponse failedEntry : getFailedEntries(response.get())) {
        addToFailedEntry(failedEntry, documents);
      }
    }

    failedEntries.forEach(this::sendToDlq);
  }

  private static Try<EventReference> getEventReference(String messageBody) {
    return attempt(() -> JsonUtils.dtoObjectMapper.readValue(messageBody, EventReference.class));
  }

  private static MessageAttributeValue toAttributeValueString(Exception exception) {
    return MessageAttributeValue.builder()
        .stringValue(exception.getMessage())
        .dataType("String")
        .build();
  }

  private static List<BulkItemResponse> getFailedEntries(Stream<BulkResponse> response) {
    return response
        .filter(BulkResponse::hasFailures)
        .map(BulkResponse::getItems)
        .map(Arrays::asList)
        .flatMap(List::stream)
        .filter(BulkItemResponse::isFailed)
        .toList();
  }

  private static IndexDocument getDocumentByIdentifier(
      Collection<IndexDocument> indexDocuments, String documentId) {
    return indexDocuments.stream()
        .filter(item -> item.getDocumentIdentifier().equals(documentId))
        .findFirst()
        .orElseThrow();
  }

  private static SendMessageRequest createDlqMessage(String messageBody, Exception exception) {
    return SendMessageRequest.builder()
        .messageBody(messageBody)
        .messageAttributes(Map.of("exception", toAttributeValueString(exception)))
        .queueUrl(new Environment().readEnv(DLQ_QUEUE))
        .build();
  }

  private void addToFailedEntry(BulkItemResponse failedEntry, Collection<IndexDocument> documents) {
    var documentIdentifier = failedEntry.getId();
    var document = getDocumentByIdentifier(documents, documentIdentifier);
    var message = documentIdToMessageMap.get(document.getDocumentIdentifier());
    failedEntries.put(message, failedEntry.getFailure().getCause());
    LOGGER.error(
        FAILED_DOCUMENT_MESSAGE, document.getDocumentIdentifier(), document.getIndexName());
  }

  private void addAllDocumentsToFailedEntries(
      Collection<IndexDocument> documents, Try<Stream<BulkResponse>> response) {
    for (IndexDocument indexDocument : documents) {
      failedEntries.put(
          documentIdToMessageMap.get(indexDocument.getDocumentIdentifier()),
          response.getException());
    }
  }

  private List<IndexDocument> fetchDocuments(Collection<String> messages) {
    var documentsToIndex = new ArrayList<IndexDocument>();
    for (String message : messages) {
      LOGGER.info("Fetching document from SQS queue for message: {}", message);
      var eventReference = getEventReference(message);
      if (eventReference.isFailure()) {
        LOGGER.error(FAILED_TO_PARSE_SQS_MESSAGE, message, eventReference.getException());
        failedEntries.put(message, eventReference.getException());
      } else {
        var document = getDocumentFromS3(eventReference);
        if (document.isFailure()) {
          LOGGER.error(FAILED_TO_FETCH_FROM_S3_MESSAGE, message, document.getException());
          failedEntries.put(message, document.getException());
        } else {
          documentsToIndex.add(document.get());
          documentIdToMessageMap.put(document.get().getDocumentIdentifier(), message);
        }
      }
    }
    return documentsToIndex;
  }

  private Try<IndexDocument> getDocumentFromS3(Try<EventReference> eventReference) {
    return attempt(() -> fetchIndexDocumentFromS3(eventReference.get()));
  }

  private void sendToDlq(String messageBody, Exception exception) {
    LOGGER.info("Sending message to dlq: {}", messageBody);
    queueClient.sendMessage(createDlqMessage(messageBody, exception));
  }

  private IndexDocument fetchIndexDocumentFromS3(EventReference eventReference) {
    var path = UriWrapper.fromUri(eventReference.getUri()).toS3bucketPath();
    return IndexDocument.fromJsonString(s3Driver.getFile(path));
  }
}
