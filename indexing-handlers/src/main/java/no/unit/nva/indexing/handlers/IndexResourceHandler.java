package no.unit.nva.indexing.handlers;

import static nva.commons.core.attempt.Try.attempt;
import com.amazonaws.services.lambda.runtime.Context;
import no.unit.nva.events.handlers.DestinationsEventBridgeEventHandler;
import no.unit.nva.events.models.AwsEventBridgeDetail;
import no.unit.nva.events.models.AwsEventBridgeEvent;
import no.unit.nva.events.models.EventReference;
import no.unit.nva.s3.S3Driver;
import no.unit.nva.search.IndexingClient;
import no.unit.nva.search.IndexingConfig;
import no.unit.nva.search.models.IndexDocument;
import nva.commons.core.JacocoGenerated;
import nva.commons.core.paths.UnixPath;
import nva.commons.core.paths.UriWrapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class IndexResourceHandler extends DestinationsEventBridgeEventHandler<EventReference, Void> {

    private static final Logger LOGGER = LoggerFactory.getLogger(IndexResourceHandler.class);
    private static final String EXPANDED_RESOURCES_BUCKET = IndexingConfig.ENVIRONMENT.readEnv(
        "EXPANDED_RESOURCES_BUCKET");
    public static final String INDEXING_ERROR_MSG = "Failure adding document to index. Resource path: {}. "
                                                    + "Exception message: {}";
    private final S3Driver s3Driver;
    private final IndexingClient indexingClient;

    @JacocoGenerated
    public IndexResourceHandler() {
        this(new S3Driver(EXPANDED_RESOURCES_BUCKET), defaultIndexingClient());
    }

    public IndexResourceHandler(S3Driver s3Driver, IndexingClient indexingClient) {
        super(EventReference.class);
        this.s3Driver = s3Driver;
        this.indexingClient = indexingClient;
    }

    @JacocoGenerated
    public static IndexingClient defaultIndexingClient() {
        return IndexingClient.defaultIndexingClient();
    }

    @Override
    protected Void processInputPayload(EventReference input,
                                       AwsEventBridgeEvent<AwsEventBridgeDetail<EventReference>> event,
                                       Context context) {

        UnixPath resourceRelativePath = UriWrapper.fromUri(input.getUri()).toS3bucketPath();
        IndexDocument indexDocument = fetchFileFromS3Bucket(resourceRelativePath).validate();
        attempt(() -> indexingClient.addDocumentToIndex(indexDocument)).orElseThrow(
            failure -> handleFailure(resourceRelativePath, failure.getException()));
        return null;
    }

    private RuntimeException handleFailure(UnixPath resourceRelativePath, Exception exception) {
        LOGGER.error(INDEXING_ERROR_MSG, resourceRelativePath, exception.getMessage());
        return new RuntimeException(exception);
    }

    private IndexDocument fetchFileFromS3Bucket(UnixPath resourceRelativePath) {
        String resource = s3Driver.getFile(resourceRelativePath);
        return IndexDocument.fromJsonString(resource);
    }
}
