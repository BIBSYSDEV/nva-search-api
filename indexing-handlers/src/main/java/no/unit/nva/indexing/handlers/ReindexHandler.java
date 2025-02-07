package no.unit.nva.indexing.handlers;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestStreamHandler;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Path;
import no.unit.nva.indexing.model.ReindexRequest;
import no.unit.nva.indexingclient.IndexingClient;
import no.unit.nva.indexingclient.ReindexingException;
import nva.commons.core.JacocoGenerated;
import nva.commons.core.ioutils.IoUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ReindexHandler implements RequestStreamHandler {

  public static final String REINDEXING_COMPLETED_MESSAGE = "Reindexing from {} to {} completed";
  public static final String REINDEXING_FAILED_MESSAGE = "Reindexing failed {}";
  private static final Logger logger = LoggerFactory.getLogger(ReindexHandler.class);
  private final IndexingClient indexingClient;

  @JacocoGenerated
  public ReindexHandler() {
    this(IndexingClient.defaultIndexingClient());
  }

  public ReindexHandler(IndexingClient indexingClient) {
    this.indexingClient = indexingClient;
  }

  @Override
  public void handleRequest(InputStream inputStream, OutputStream outputStream, Context context)
      throws IOException {
    try {
      var request = ReindexRequest.fromInputStream(inputStream);
      var mappings = IoUtils.stringFromResources(Path.of("resource_mappings.json"));
      indexingClient.reindex(request.oldIndex(), request.newIndex(), mappings);
      logger.info(REINDEXING_COMPLETED_MESSAGE, request.oldIndex(), request.newIndex());
    } catch (ReindexingException exception) {
      logger.error(REINDEXING_FAILED_MESSAGE, exception.getMessage());
      throw new ReindexingException(exception.getMessage());
    }
  }
}
