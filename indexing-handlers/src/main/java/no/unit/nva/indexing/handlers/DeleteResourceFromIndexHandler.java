package no.unit.nva.indexing.handlers;

import com.amazonaws.services.lambda.runtime.Context;
import no.unit.nva.events.handlers.DestinationsEventBridgeEventHandler;
import no.unit.nva.events.models.AwsEventBridgeDetail;
import no.unit.nva.events.models.AwsEventBridgeEvent;
import no.unit.nva.search.IndexingClient;
import nva.commons.core.JacocoGenerated;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DeleteResourceFromIndexHandler extends DestinationsEventBridgeEventHandler<DeleteResourceEvent, Void> {

    private static final Logger logger = LoggerFactory.getLogger(DeleteResourceFromIndexHandler.class);

    private final IndexingClient indexingClient;

    @JacocoGenerated
    public DeleteResourceFromIndexHandler() {
        this(IndexingClient.defaultIndexingClient());
    }

    public DeleteResourceFromIndexHandler(IndexingClient indexingClient) {
        super(DeleteResourceEvent.class);
        this.indexingClient = indexingClient;
    }

    @Override
    protected Void processInputPayload(DeleteResourceEvent input,
                                       AwsEventBridgeEvent<AwsEventBridgeDetail<DeleteResourceEvent>> event,
                                       Context context) {
        try {
            indexingClient.removeDocumentFromResourcesIndex(input.getIdentifier().toString());
        } catch (Exception e) {
            logError(e);
            throw new RuntimeException(e);
        }

        return null;
    }

    private void logError(Exception exception) {
        logger.warn("Removing document failed", exception);
    }
}
