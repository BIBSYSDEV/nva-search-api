package no.unit.nva.indexing.handlers;

import static no.unit.nva.constants.Words.DOIREQUESTS_INDEX;
import static no.unit.nva.constants.Words.MESSAGES_INDEX;
import static no.unit.nva.constants.Words.PUBLISHING_REQUESTS_INDEX;
import static no.unit.nva.constants.Words.RESOURCES;
import static no.unit.nva.constants.Words.TICKETS;
import static nva.commons.core.attempt.Try.attempt;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import no.unit.nva.indexingclient.IndexingClient;
import nva.commons.core.JacocoGenerated;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DeleteIndicesHandler implements RequestHandler<Object, String> {

  private static final Logger logger = LoggerFactory.getLogger(DeleteIndicesHandler.class);
  private static final String FINISHED = "FINISHED";
  private final IndexingClient indexingClient;

  @JacocoGenerated
  public DeleteIndicesHandler() {
    this(IndexingClient.defaultIndexingClient());
  }

  public DeleteIndicesHandler(IndexingClient indexingClient) {
    this.indexingClient = indexingClient;
  }

  @Override
  public String handleRequest(Object input, Context context) {

    attempt(() -> indexingClient.deleteIndex(RESOURCES))
        .orElse(fail -> logError(fail.getException()));
    attempt(() -> indexingClient.deleteIndex(DOIREQUESTS_INDEX))
        .orElse(fail -> logError(fail.getException()));
    attempt(() -> indexingClient.deleteIndex(MESSAGES_INDEX))
        .orElse(fail -> logError(fail.getException()));
    attempt(() -> indexingClient.deleteIndex(PUBLISHING_REQUESTS_INDEX))
        .orElse(fail -> logError(fail.getException()));
    attempt(() -> indexingClient.deleteIndex(TICKETS))
        .orElse(fail -> logError(fail.getException()));

    return FINISHED;
  }

  private Void logError(Exception exception) {
    logger.warn("Index deletion failed", exception);
    return null;
  }
}
