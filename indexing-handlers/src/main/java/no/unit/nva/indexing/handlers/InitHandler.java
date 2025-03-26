package no.unit.nva.indexing.handlers;

import static no.unit.nva.constants.IndexMappingsAndSettings.RESOURCE_MAPPINGS;
import static no.unit.nva.constants.IndexMappingsAndSettings.RESOURCE_SETTINGS;
import static no.unit.nva.constants.IndexMappingsAndSettings.TICKET_MAPPINGS;
import static no.unit.nva.constants.Words.PUBLISHING_REQUESTS_INDEX;
import static no.unit.nva.constants.Words.RESOURCES;
import static no.unit.nva.constants.Words.TICKETS;
import static no.unit.nva.indexingclient.IndexingClient.defaultIndexingClient;
import static nva.commons.core.attempt.Try.attempt;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import no.unit.nva.indexing.model.IndexRequest;
import no.unit.nva.indexingclient.IndexingClient;
import nva.commons.core.JacocoGenerated;
import nva.commons.core.attempt.Failure;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class InitHandler implements RequestHandler<Object, String> {

  public static final String SUCCESS = "SUCCESS";
  public static final String FAILED = "FAILED. See logs";

  private static final List<IndexRequest> INDEXES =
      List.of(
          new IndexRequest(RESOURCES, RESOURCE_MAPPINGS.asJson(), RESOURCE_SETTINGS.asJson()),
          new IndexRequest(TICKETS, TICKET_MAPPINGS.asJson()),
          new IndexRequest(PUBLISHING_REQUESTS_INDEX));
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

    var failState = new AtomicBoolean(false);

    INDEXES.forEach(
        request ->
            attempt(
                    () ->
                        indexingClient.createIndex(
                            request.getName(), request.getMappings(), request.getSettings()))
                .orElse(fail -> handleFailure(failState, fail)));

    return failState.get() ? FAILED : SUCCESS;
  }

  private Void handleFailure(AtomicBoolean failState, Failure<?> failure) {
    failState.set(true);
    logger.warn("Index creation failed", failure.getException());
    return null;
  }
}
