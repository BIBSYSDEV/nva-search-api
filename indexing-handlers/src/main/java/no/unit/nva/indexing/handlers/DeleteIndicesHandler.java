package no.unit.nva.indexing.handlers;

import static no.unit.nva.constants.Words.RESOURCES;
import static no.unit.nva.constants.Words.TICKETS;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import java.util.List;
import no.unit.nva.indexingclient.IndexingClient;
import nva.commons.core.JacocoGenerated;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DeleteIndicesHandler implements RequestHandler<Object, String> {

  private static final Logger logger = LoggerFactory.getLogger(DeleteIndicesHandler.class);
  private static final String SUCCESS = "SUCCESS";
  private static final String FAILED = "FAILED. See logs";
  private static final List<String> INDICES_TO_DELETE = List.of(RESOURCES, TICKETS);
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
    logger.info("Starting index deletion for indices: {}", INDICES_TO_DELETE);

    boolean hasFailed = false;
    for (var indexName : INDICES_TO_DELETE) {
      logger.info("Attempting to delete index '{}'", indexName);
      try {
        indexingClient.deleteIndex(indexName);
        logger.info("Deleted index '{}'", indexName);
      } catch (Exception exception) {
        logger.error("Failed to delete index '{}'", indexName, exception);
        hasFailed = true;
      }
    }
    logger.info("Index deletion completed");
    return hasFailed ? FAILED : SUCCESS;
  }
}
