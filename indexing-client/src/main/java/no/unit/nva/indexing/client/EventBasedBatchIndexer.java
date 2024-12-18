package no.unit.nva.indexing.client;

import static no.unit.nva.indexing.client.Constants.NUMBER_OF_FILES_PER_EVENT_ENVIRONMENT_VARIABLE;
import static no.unit.nva.indexing.client.Constants.RECURSION_ENABLED;
import static no.unit.nva.indexing.client.Constants.defaultEsClient;
import static no.unit.nva.indexing.client.Constants.defaultEventBridgeClient;
import static no.unit.nva.indexing.client.Constants.defaultS3Client;
import static no.unit.nva.indexing.client.EmitEventUtils.emitEvent;

import com.amazonaws.services.lambda.runtime.Context;

import no.unit.nva.events.handlers.EventHandler;
import no.unit.nva.events.models.AwsEventBridgeEvent;
import no.unit.nva.identifiers.SortableIdentifier;
import no.unit.nva.search.model.IndexingClient;

import nva.commons.core.JacocoGenerated;
import nva.commons.core.ioutils.IoUtils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import software.amazon.awssdk.services.eventbridge.EventBridgeClient;
import software.amazon.awssdk.services.s3.S3Client;

import java.io.InputStream;
import java.io.OutputStream;

public class EventBasedBatchIndexer
        extends EventHandler<ImportDataRequestEvent, SortableIdentifier[]> {

    private static final Logger logger = LoggerFactory.getLogger(EventBasedBatchIndexer.class);

    private final S3Client s3Client;
    private final IndexingClient openSearchClient;
    private final EventBridgeClient eventBridgeClient;
    private final int numberOfFilesPerEvent;

    @JacocoGenerated
    public EventBasedBatchIndexer() {
        this(
                defaultS3Client(),
                defaultEsClient(),
                defaultEventBridgeClient(),
                NUMBER_OF_FILES_PER_EVENT_ENVIRONMENT_VARIABLE);
    }

    protected EventBasedBatchIndexer(
            S3Client s3Client,
            IndexingClient openSearchClient,
            EventBridgeClient eventBridgeClient,
            int numberOfFilesPerEvent) {
        super(ImportDataRequestEvent.class);
        this.s3Client = s3Client;
        this.openSearchClient = openSearchClient;
        this.eventBridgeClient = eventBridgeClient;
        this.numberOfFilesPerEvent = numberOfFilesPerEvent;
    }

    @Override
    public void handleRequest(InputStream inputStream, OutputStream outputStream, Context context) {
        String inputString = IoUtils.streamToString(inputStream);
        logger.debug(inputString);
        super.handleRequest(IoUtils.stringToStream(inputString), outputStream, context);
    }

    @Override
    protected SortableIdentifier[] processInput(
            ImportDataRequestEvent input,
            AwsEventBridgeEvent<ImportDataRequestEvent> event,
            Context context) {
        logger.debug("Indexing folder:" + input.getS3Location());
        logger.debug("Indexing lastEvaluatedKey:" + input.getStartMarker());
        IndexingResult<SortableIdentifier> result =
                new BatchIndexer(input, s3Client, openSearchClient, numberOfFilesPerEvent)
                        .processRequest();
        if (result.truncated() && RECURSION_ENABLED) {
            emitEventToProcessNextBatch(input, context, result);
        }
        return result.failedResults().toArray(SortableIdentifier[]::new);
    }

    private void emitEventToProcessNextBatch(
            ImportDataRequestEvent input,
            Context context,
            IndexingResult<SortableIdentifier> result) {
        ImportDataRequestEvent newImportDataRequest =
                new ImportDataRequestEvent(input.getS3Location(), result.nextStartMarker());
        emitEvent(eventBridgeClient, newImportDataRequest, context);
    }
}
