package no.unit.nva.indexing.handlers;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestStreamHandler;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Set;
import java.util.stream.Collectors;
import no.unit.nva.indexingclient.IndexingClient;
import nva.commons.core.JacocoGenerated;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DeleteIndicesHandler implements RequestStreamHandler {

  private static final String MISSING_INDEX_TO_DELETE_MESSAGE =
      "Provide at least one index to delete!";
  private static final Logger logger = LoggerFactory.getLogger(DeleteIndicesHandler.class);
  private final IndexingClient indexingClient;

  @JacocoGenerated
  public DeleteIndicesHandler() {
    this(IndexingClient.defaultIndexingClient());
  }

  public DeleteIndicesHandler(IndexingClient indexingClient) {
    this.indexingClient = indexingClient;
  }

  @Override
  public void handleRequest(InputStream input, OutputStream output, Context context)
      throws IOException {
    var indicesToDelete = getIndicesToDelete(input);
    logger.info("Starting index deletion for indices: {}", indicesToDelete);

    for (var indexName : indicesToDelete) {
      logger.info("Attempting to delete index '{}'", indexName);
      try {
        indexingClient.deleteIndex(indexName);
        logger.info("Deleted index '{}'", indexName);
      } catch (Exception exception) {
        logger.error("Failed to delete index '{}'", indexName, exception);
      }
    }
    logger.info("Index deletion completed");
  }

  private static Set<String> getIndicesToDelete(InputStream inputStream) {
    var indices = DeleteIndicesRequest.fromInputStream(inputStream).indices();
    if (indices.isEmpty()) {
      throw new IllegalStateException(MISSING_INDEX_TO_DELETE_MESSAGE);
    }
    return indices.stream().map(IndexName::getValue).collect(Collectors.toSet());
  }
}
