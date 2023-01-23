package no.unit.nva.indexing.handlers;

import static no.unit.nva.s3.S3Driver.defaultS3Client;
import com.amazonaws.services.lambda.runtime.Context;
import no.unit.nva.events.handlers.DestinationsEventBridgeEventHandler;
import no.unit.nva.events.models.AwsEventBridgeDetail;
import no.unit.nva.events.models.AwsEventBridgeEvent;
import no.unit.nva.events.models.EventReference;
import no.unit.nva.s3.S3Driver;
import no.unit.nva.search.IndexingClient;
import nva.commons.core.JacocoGenerated;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.amazon.awssdk.services.s3.S3Client;

public class DeleteResourceFromIndexHandler extends DestinationsEventBridgeEventHandler<EventReference, Void> {

    private static final Logger logger = LoggerFactory.getLogger(DeleteResourceFromIndexHandler.class);

    private final IndexingClient indexingClient;

    private final S3Client s3Client;

    @JacocoGenerated
    public DeleteResourceFromIndexHandler() {
        this(IndexingClient.defaultIndexingClient(), defaultS3Client().build());
    }

    public DeleteResourceFromIndexHandler(IndexingClient indexingClient, S3Client s3Client) {
        super(EventReference.class);
        this.indexingClient = indexingClient;
        this.s3Client = s3Client;
    }

    @Override
    protected Void processInputPayload(EventReference input,
                                       AwsEventBridgeEvent<AwsEventBridgeDetail<EventReference>> event,
                                       Context context) {
        logger.info("INPUT toString: " + input.toString());
        logger.info("Input json string" + input.toJsonString());
        var eventbody = readEventBody(input);
        try {
            indexingClient.removeDocumentFromIndex(eventbody.getIdentifier().toString());
        } catch (Exception e) {
            logError(e);
            throw new RuntimeException(e);
        }

        return null;
    }

    private DeleteResourceEvent readEventBody(EventReference input) {
        var s3Driver = new S3Driver(s3Client, input.extractBucketName());
        var json = s3Driver.readEvent(input.getUri());
        return DeleteResourceEvent.fromJson(json);
    }

    private void logError(Exception exception) {
        logger.warn("Removing document failed", exception);
    }
}
