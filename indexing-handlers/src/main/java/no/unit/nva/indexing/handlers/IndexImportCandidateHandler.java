package no.unit.nva.indexing.handlers;

import static nva.commons.core.attempt.Try.attempt;

import com.amazonaws.services.lambda.runtime.Context;
import no.unit.nva.events.handlers.DestinationsEventBridgeEventHandler;
import no.unit.nva.events.models.AwsEventBridgeDetail;
import no.unit.nva.events.models.AwsEventBridgeEvent;
import no.unit.nva.events.models.EventReference;
import no.unit.nva.indexingclient.IndexingClient;
import no.unit.nva.indexingclient.models.IndexDocument;
import no.unit.nva.s3.S3Driver;
import nva.commons.core.Environment;
import nva.commons.core.JacocoGenerated;
import nva.commons.core.paths.UnixPath;
import nva.commons.core.paths.UriWrapper;

public class IndexImportCandidateHandler
    extends DestinationsEventBridgeEventHandler<EventReference, Void> {

  private static final String EXPANDED_RESOURCES_BUCKET =
      new Environment().readEnv("EXPANDED_RESOURCES_BUCKET");
  private final S3Driver s3Driver;
  private final IndexingClient indexingClient;

  @JacocoGenerated
  public IndexImportCandidateHandler() {
    this(new S3Driver(EXPANDED_RESOURCES_BUCKET), defaultIndexingClient());
  }

  public IndexImportCandidateHandler(S3Driver s3Driver, IndexingClient indexingClient) {
    super(EventReference.class);
    this.s3Driver = s3Driver;
    this.indexingClient = indexingClient;
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
    attempt(() -> indexingClient.addDocumentToIndex(indexDocument)).orElseThrow();
    return null;
  }

  private IndexDocument fetchFileFromS3Bucket(UnixPath resourceRelativePath) {
    var resource = s3Driver.getFile(resourceRelativePath);
    return IndexDocument.fromJsonString(resource);
  }
}
