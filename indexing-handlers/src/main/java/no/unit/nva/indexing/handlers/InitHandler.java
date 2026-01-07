package no.unit.nva.indexing.handlers;

import static no.unit.nva.constants.IndexMappingsAndSettings.RESOURCE_MAPPINGS;
import static no.unit.nva.constants.IndexMappingsAndSettings.RESOURCE_SETTINGS;
import static no.unit.nva.constants.IndexMappingsAndSettings.TICKET_MAPPINGS;
import static no.unit.nva.constants.Words.RESOURCES;
import static no.unit.nva.constants.Words.TICKETS;
import static no.unit.nva.indexingclient.IndexingClient.defaultIndexingClient;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import java.util.List;
import no.unit.nva.indexing.model.IndexRequest;
import no.unit.nva.indexingclient.IndexingClient;
import nva.commons.core.JacocoGenerated;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class InitHandler implements RequestHandler<Object, String> {

  public static final String SUCCESS = "SUCCESS";
  public static final String FAILED = "FAILED. See logs";

  private static final List<IndexRequest> INDICES_TO_CREATE =
      List.of(
          new IndexRequest(RESOURCES, RESOURCE_MAPPINGS.asJson(), RESOURCE_SETTINGS.asJson()),
          new IndexRequest(TICKETS, TICKET_MAPPINGS.asJson()));
  private static final Logger logger = LoggerFactory.getLogger(InitHandler.class);
  private final IndexingClient indexingClient;

  @JacocoGenerated
  public InitHandler() {
    this(defaultIndexingClient());
  }

  public InitHandler(IndexingClient indexingClient) {
    this.indexingClient = indexingClient;
  }

  @Override
  public String handleRequest(Object input, Context context) {
    var indexNames = INDICES_TO_CREATE.stream().map(IndexRequest::getName).toList();
    logger.info("Starting index creation for indices: {}", indexNames);

    boolean hasFailed = false;
    for (var request : INDICES_TO_CREATE) {
      var indexName = request.getName();
      logger.info("Attempting to create index '{}'", indexName);
      try {
        indexingClient.createIndex(indexName, request.getMappings(), request.getSettings());
        logger.info("Created index '{}'", indexName);
      } catch (Exception exception) {
        logger.error("Failed to create index '{}'", indexName, exception);
        hasFailed = true;
      }
    }
    logger.info("Index creation completed");
    return hasFailed ? FAILED : SUCCESS;
  }
}
