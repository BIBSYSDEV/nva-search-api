package no.unit.nva.indexing.handlers;

import static no.unit.nva.search.model.constant.Defaults.objectMapperWithEmpty;
import static no.unit.nva.search.model.constant.ErrorMessages.MISSING_IDENTIFIER_IN_RESOURCE;
import static no.unit.nva.search.model.constant.ErrorMessages.MISSING_INDEX_NAME_IN_RESOURCE;
import static no.unit.nva.search.model.constant.Words.IMPORT_CANDIDATES_INDEX;
import static no.unit.nva.testutils.RandomDataGenerator.randomJson;
import static no.unit.nva.testutils.RandomDataGenerator.randomString;
import static nva.commons.core.attempt.Try.attempt;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.stringContainsInOrder;
import static org.hamcrest.collection.IsIterableContainingInOrder.contains;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.amazonaws.services.lambda.runtime.Context;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import no.unit.nva.events.models.AwsEventBridgeDetail;
import no.unit.nva.events.models.AwsEventBridgeEvent;
import no.unit.nva.events.models.EventReference;
import no.unit.nva.identifiers.SortableIdentifier;
import no.unit.nva.indexing.testutils.FakeIndexingClient;
import no.unit.nva.s3.S3Driver;
import no.unit.nva.search.model.records.EventConsumptionAttributes;
import no.unit.nva.search.model.records.IndexDocument;
import no.unit.nva.stubs.FakeS3Client;
import no.unit.nva.testutils.RandomDataGenerator;
import nva.commons.core.paths.UriWrapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import software.amazon.awssdk.services.s3.model.NoSuchKeyException;

public class IndexImportCandidateHandlerTest {

  public static final IndexDocument SAMPLE_RESOURCE =
      createSampleResource(SortableIdentifier.next(), IMPORT_CANDIDATES_INDEX);
  public static final String FILE_DOES_NOT_EXIST = "File does not exist";
  public static final String IGNORED_TOPIC = "ignoredValue";
  private static final IndexDocument SAMPLE_IMPORT_CANDIDATE_MISSING_IDENTIFIER =
      createSampleResource(null, IMPORT_CANDIDATES_INDEX);
  private static final IndexDocument IMPORT_CANDIDATE_MISSING_INDEX_NAME =
      createSampleResource(SortableIdentifier.next(), null);
  private S3Driver s3Driver;
  private IndexImportCandidateHandler handler;
  private Context context;
  private ByteArrayOutputStream output;
  private FakeIndexingClient indexingClient;

  private static IndexDocument createSampleResource(
      SortableIdentifier identifierProvider, String indexName) {
    var randomJson = randomJson();
    var objectNode =
        attempt(() -> (ObjectNode) objectMapperWithEmpty.readTree(randomJson)).orElseThrow();
    var metadata = new EventConsumptionAttributes(indexName, identifierProvider);
    return new IndexDocument(metadata, objectNode);
  }

  @BeforeEach
  void init() {
    FakeS3Client fakeS3Client = new FakeS3Client();
    s3Driver = new S3Driver(fakeS3Client, "ignored");
    indexingClient = new FakeIndexingClient();
    handler = new IndexImportCandidateHandler(s3Driver, indexingClient);

    context = Mockito.mock(Context.class);
    output = new ByteArrayOutputStream();
  }

  @Test
  void shouldAddDocumentToIndexWhenResourceExistsInResourcesStorage() throws Exception {
    var resourceLocation = prepareEventStorageResourceFile();
    var input = attempt(() -> createEventBridgeEvent(resourceLocation)).get();
    handler.handleRequest(input, output, context);
    var allIndexedDocuments = indexingClient.listAllDocuments(SAMPLE_RESOURCE.getIndexName());

    assertThat(allIndexedDocuments, contains(SAMPLE_RESOURCE.resource()));
  }

  @Test
  void shouldThrowExceptionOnCommunicationProblemWithService() throws Exception {
    final var expectedErrorMessage = randomString();
    indexingClient = indexingClientThrowingException(expectedErrorMessage);
    handler = new IndexImportCandidateHandler(s3Driver, indexingClient);
    var resourceLocation = prepareEventStorageResourceFile();
    var input = attempt(() -> createEventBridgeEvent(resourceLocation)).get();

    assertThrows(RuntimeException.class, () -> handler.handleRequest(input, output, context));
  }

  @Test
  void shouldThrowExceptionWhenResourceIsMissingIdentifier() throws Exception {
    var resourceLocation =
        prepareEventStorageResourceFile(SAMPLE_IMPORT_CANDIDATE_MISSING_IDENTIFIER);
    var input = attempt(() -> createEventBridgeEvent(resourceLocation)).get();
    var exception =
        assertThrows(RuntimeException.class, () -> handler.handleRequest(input, output, context));

    assertThat(exception.getMessage(), stringContainsInOrder(MISSING_IDENTIFIER_IN_RESOURCE));
  }

  @Test
  void shouldThrowNoSuchKeyExceptionWhenResourceIsMissingFromEventStorage() {
    var missingResourceLocation = RandomDataGenerator.randomUri();
    var input = attempt(() -> createEventBridgeEvent(missingResourceLocation)).get();
    var exception =
        assertThrows(NoSuchKeyException.class, () -> handler.handleRequest(input, output, context));

    assertThat(exception.getMessage(), stringContainsInOrder(FILE_DOES_NOT_EXIST));
  }

  @Test
  void shouldThrowExceptionWhenEventConsumptionAttributesIsMissingIndexName() throws Exception {
    var resourceLocation = prepareEventStorageResourceFile(IMPORT_CANDIDATE_MISSING_INDEX_NAME);
    var input = attempt(() -> createEventBridgeEvent(resourceLocation)).get();
    var exception =
        assertThrows(RuntimeException.class, () -> handler.handleRequest(input, output, context));

    assertThat(exception.getMessage(), stringContainsInOrder(MISSING_INDEX_NAME_IN_RESOURCE));
  }

  private FakeIndexingClient indexingClientThrowingException(String expectedErrorMessage) {
    return new FakeIndexingClient() {
      @Override
      public Void addDocumentToIndex(IndexDocument indexDocument) throws IOException {
        throw new IOException(expectedErrorMessage);
      }
    };
  }

  private URI prepareEventStorageResourceFile() throws IOException {
    return prepareEventStorageResourceFile(SAMPLE_RESOURCE);
  }

  private URI prepareEventStorageResourceFile(IndexDocument resource) throws IOException {
    var resourceLocation = RandomDataGenerator.randomUri();
    var resourcePath = UriWrapper.fromUri(resourceLocation).toS3bucketPath();
    s3Driver.insertFile(resourcePath, resource.toJsonString());
    return resourceLocation;
  }

  private InputStream createEventBridgeEvent(URI resourceLocation) throws JsonProcessingException {
    var indexResourceEvent = new EventReference(IGNORED_TOPIC, resourceLocation);
    var detail = new AwsEventBridgeDetail<>();
    detail.setResponsePayload(indexResourceEvent);
    var event = new AwsEventBridgeEvent<>();
    event.setDetail(detail);
    return new ByteArrayInputStream(objectMapperWithEmpty.writeValueAsBytes(event));
  }
}
