package no.unit.nva.indexing.handlers;

import static no.unit.nva.search.model.constant.Defaults.ENVIRONMENT;

import static nva.commons.core.attempt.Try.attempt;

import com.amazonaws.services.lambda.runtime.Context;

import no.unit.nva.events.handlers.DestinationsEventBridgeEventHandler;
import no.unit.nva.events.models.AwsEventBridgeDetail;
import no.unit.nva.events.models.AwsEventBridgeEvent;
import no.unit.nva.events.models.EventReference;
import no.unit.nva.indexing.utils.RecoveryEntry;
import no.unit.nva.search.model.IndexQueueClient;
import no.unit.nva.search.model.IndexingClient;
import no.unit.nva.search.model.records.IndexDocument;
import no.unit.nva.search.model.records.QueueClient;
import no.unit.nva.s3.S3Driver;

import nva.commons.core.JacocoGenerated;
import nva.commons.core.attempt.Failure;
import nva.commons.core.paths.UnixPath;
import nva.commons.core.paths.UriWrapper;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class IndexResourceHandler
        extends DestinationsEventBridgeEventHandler<EventReference, Void> {

    private static final Logger LOGGER = LoggerFactory.getLogger(IndexResourceHandler.class);
    private static final String EXPANDED_RESOURCES_BUCKET =
            ENVIRONMENT.readEnv("EXPANDED_RESOURCES_BUCKET");
    private static final String SENT_TO_RECOVERY_QUEUE_MESSAGE =
            "IndexDocument for index {} has been sent to recovery queue: {}";
    public static final String INDEXING_MESSAGE = "Indexing document with id: {} to {}";

    private final S3Driver resourcesS3Driver;
    private final IndexingClient indexingClient;
    private final QueueClient queueClient;

    @JacocoGenerated
    public IndexResourceHandler() {
        this(
                new S3Driver(EXPANDED_RESOURCES_BUCKET),
                defaultIndexingClient(),
                IndexQueueClient.defaultQueueClient());
    }

    public IndexResourceHandler(
            S3Driver resourcesS3Driver, IndexingClient indexingClient, QueueClient queueClient) {
        super(EventReference.class);
        this.resourcesS3Driver = resourcesS3Driver;
        this.indexingClient = indexingClient;
        this.queueClient = queueClient;
    }

    @JacocoGenerated
    public static IndexingClient defaultIndexingClient() {
        return IndexingClient.defaultIndexingClient();
    }

    @Override
    protected Void processInputPayload(
            EventReference input,
            AwsEventBridgeEvent<AwsEventBridgeDetail<EventReference>> event,
            Context context) {

        var resourceRelativePath = UriWrapper.fromUri(input.getUri()).toS3bucketPath();
        var indexDocument = fetchFileFromS3Bucket(resourceRelativePath).validate();
        attempt(() -> indexingClient.addDocumentToIndex(indexDocument))
                .orElse(failure -> persistRecoveryMessage(failure, indexDocument));
        LOGGER.info(
                INDEXING_MESSAGE,
                indexDocument.getDocumentIdentifier(),
                indexDocument.getIndexName());
        return null;
    }

    private Void persistRecoveryMessage(Failure<Void> failure, IndexDocument indexDocument) {
        var documentIdentifier = indexDocument.getDocumentIdentifier();
        RecoveryEntry.fromIndexDocument(indexDocument)
                .withIdentifier(documentIdentifier)
                .withException(failure.getException())
                .persist(queueClient);
        LOGGER.error(
                SENT_TO_RECOVERY_QUEUE_MESSAGE, indexDocument.getIndexName(), documentIdentifier);
        return null;
    }

    private IndexDocument fetchFileFromS3Bucket(UnixPath resourceRelativePath) {
        var resource = resourcesS3Driver.getFile(resourceRelativePath);
        return IndexDocument.fromJsonString(resource);
    }
}
