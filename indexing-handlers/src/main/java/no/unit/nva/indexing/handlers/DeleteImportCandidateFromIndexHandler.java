package no.unit.nva.indexing.handlers;

import com.amazonaws.services.lambda.runtime.Context;
import no.unit.nva.events.handlers.DestinationsEventBridgeEventHandler;
import no.unit.nva.events.models.AwsEventBridgeDetail;
import no.unit.nva.events.models.AwsEventBridgeEvent;
import no.unit.nva.indexing.model.DeleteImportCandidateEvent;
import no.unit.nva.search.IndexingClient;
import nva.commons.core.JacocoGenerated;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DeleteImportCandidateFromIndexHandler
    extends DestinationsEventBridgeEventHandler<DeleteImportCandidateEvent, Void> {

    private static final Logger logger = LoggerFactory.getLogger(DeleteImportCandidateFromIndexHandler.class);
    public static final String REMOVING_DOCUMENT_FAILED_MESSAGE = "Removing document failed";
    private final IndexingClient indexingClient;

    @JacocoGenerated
    public DeleteImportCandidateFromIndexHandler() {
        this(IndexingClient.defaultIndexingClient());
    }

    public DeleteImportCandidateFromIndexHandler(IndexingClient indexingClient) {
        super(DeleteImportCandidateEvent.class);
        this.indexingClient = indexingClient;
    }

    @Override
    protected Void processInputPayload(DeleteImportCandidateEvent input,
                                       AwsEventBridgeEvent<AwsEventBridgeDetail<DeleteImportCandidateEvent>> event,
                                       Context context) {
        try {
            indexingClient.removeDocumentFromImportCandidateIndex(input.getIdentifier().toString());
        } catch (Exception e) {
            logError(e);
            throw new RuntimeException(e);
        }

        return null;
    }

    private void logError(Exception exception) {
        logger.warn(REMOVING_DOCUMENT_FAILED_MESSAGE, exception);
    }
}
