package no.unit.nva.indexing.handlers;

import static no.unit.nva.constants.Words.IMPORT_CANDIDATES_INDEX;

import static nva.commons.core.attempt.Try.attempt;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;

import no.unit.nva.indexingclient.IndexingClient;

import nva.commons.core.JacocoGenerated;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DeleteImportCandidateIndexHandler implements RequestHandler<Object, String> {

    public static final String FINISHED = "FINISHED";
    public static final String INDEX_DELETION_FAILED_MESSAGE = "Index deletion failed";
    private static final Logger logger = LoggerFactory.getLogger(DeleteIndicesHandler.class);
    private final IndexingClient indexingClient;

    @JacocoGenerated
    public DeleteImportCandidateIndexHandler() {
        this(IndexingClient.defaultIndexingClient());
    }

    public DeleteImportCandidateIndexHandler(IndexingClient indexingClient) {
        this.indexingClient = indexingClient;
    }

    @Override
    public String handleRequest(Object input, Context context) {
        attempt(() -> indexingClient.deleteIndex(IMPORT_CANDIDATES_INDEX))
                .orElse(fail -> logError(fail.getException()));
        return FINISHED;
    }

    private Void logError(Exception exception) {
        logger.warn(INDEX_DELETION_FAILED_MESSAGE, exception);
        return null;
    }
}
