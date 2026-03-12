package no.unit.nva.indexingclient;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.SQSEvent;
import com.amazonaws.services.lambda.runtime.events.SQSEvent.SQSMessage;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import nva.commons.core.JacocoGenerated;

public class BatchIndexResourceHandler implements RequestHandler<SQSEvent, Void> {

  private final BatchIndexService batchIndexService;

  @JacocoGenerated
  public BatchIndexResourceHandler() {
    this(BatchIndexService.defaultService());
  }

  public BatchIndexResourceHandler(BatchIndexService batchIndexService) {
    this.batchIndexService = batchIndexService;
  }

  @Override
  public Void handleRequest(SQSEvent sqsEvent, Context context) {
    batchIndexService.batchIndex(getMessageBodies(sqsEvent));
    return null;
  }

  private static List<String> getMessageBodies(SQSEvent sqsEvent) {
    return Optional.ofNullable(sqsEvent).map(SQSEvent::getRecords).stream()
        .flatMap(Collection::stream)
        .map(SQSMessage::getBody)
        .toList();
  }
}
