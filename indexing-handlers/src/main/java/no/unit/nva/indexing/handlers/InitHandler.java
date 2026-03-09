package no.unit.nva.indexing.handlers;

import static no.unit.nva.constants.IndexMappingsAndSettings.IMPORT_CANDIDATE_MAPPINGS;
import static no.unit.nva.constants.IndexMappingsAndSettings.RESOURCE_MAPPINGS;
import static no.unit.nva.constants.IndexMappingsAndSettings.RESOURCE_SETTINGS;
import static no.unit.nva.constants.IndexMappingsAndSettings.TICKET_MAPPINGS;
import static no.unit.nva.constants.Words.IMPORT_CANDIDATES_INDEX;
import static no.unit.nva.constants.Words.RESOURCES;
import static no.unit.nva.constants.Words.TICKETS;
import static no.unit.nva.indexingclient.IndexingClient.defaultIndexingClient;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestStreamHandler;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Arrays;
import java.util.List;
import no.unit.nva.indexingclient.IndexingClient;
import nva.commons.core.JacocoGenerated;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class InitHandler implements RequestStreamHandler {

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
  public void handleRequest(InputStream inputStream, OutputStream outputStream, Context context)
      throws IOException {
    var indicesToCreate = getIndicesToCreate(inputStream);
    logger.info("Starting index creation for indices: {}", indicesToCreate);
    for (var indexRequest : indicesToCreate) {
      var indexName = indexRequest.name();
      logger.info("Attempting to create index '{}'", indexName);
      try {
        indexingClient.createIndex(indexName, indexRequest.mappings(), indexRequest.settings());
        logger.info("Created index '{}'", indexName);
      } catch (Exception exception) {
        logger.error("Failed to create index '{}'", indexName, exception);
      }
    }
    logger.info("Index creation completed");
  }

  private static List<IndexRequest> getIndicesToCreate(InputStream inputStream) {
    var indices = CreateIndexRequest.fromInputStream(inputStream).indices();
    return indices.isEmpty()
        ? Arrays.stream(IndexName.values()).map(InitHandler::toIndexRequest).toList()
        : indices.stream().map(InitHandler::toIndexRequest).toList();
  }

  public static IndexRequest toIndexRequest(IndexName indexName) {
    return switch (indexName) {
      case RESOURCES ->
          new IndexRequest(RESOURCES, RESOURCE_MAPPINGS.asJson(), RESOURCE_SETTINGS.asJson());
      case TICKETS -> new IndexRequest(TICKETS, TICKET_MAPPINGS.asJson());
      case IMPORT_CANDIDATES ->
          new IndexRequest(IMPORT_CANDIDATES_INDEX, IMPORT_CANDIDATE_MAPPINGS.asJson());
    };
  }
}
