package no.unit.nva.indexing.handlers;

import static no.unit.nva.constants.Words.IMPORT_CANDIDATES_INDEX;
import static nva.commons.core.attempt.Try.attempt;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import java.util.concurrent.atomic.AtomicBoolean;
import no.unit.nva.constants.IndexMappingsAndSettings;
import no.unit.nva.indexing.model.IndexRequest;
import no.unit.nva.indexingclient.IndexingClient;
import nva.commons.core.JacocoGenerated;
import nva.commons.core.attempt.Failure;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ImportCandidateInitHandler implements RequestHandler<Object, String> {

  public static final String SUCCESS = "SUCCESS";
  public static final String FAILED = "FAILED. See logs";
  private static final IndexRequest INDEX =
      new IndexRequest(
          IMPORT_CANDIDATES_INDEX, IndexMappingsAndSettings.IMPORT_CANDIDATE_MAPPINGS.asJson());
  private static final Logger logger = LoggerFactory.getLogger(InitHandler.class);
  private final IndexingClient indexingClient;

  @JacocoGenerated
  public ImportCandidateInitHandler() {
    this(IndexingClient.defaultIndexingClient());
  }

  public ImportCandidateInitHandler(IndexingClient indexingClient) {
    this.indexingClient = indexingClient;
  }

  @Override
  public String handleRequest(Object input, Context context) {
    var failState = new AtomicBoolean(false);

    attempt(() -> indexingClient.createIndex(INDEX.getName(), INDEX.getMappings()))
        .orElse(fail -> handleFailure(failState, fail));

    return failState.get() ? FAILED : SUCCESS;
  }

  private Void handleFailure(AtomicBoolean failState, Failure<?> failure) {
    failState.set(true);
    logger.warn("Index creation failed", failure.getException());
    return null;
  }
}
