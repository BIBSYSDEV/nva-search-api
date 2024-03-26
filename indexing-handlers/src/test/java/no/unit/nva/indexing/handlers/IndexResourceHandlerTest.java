package no.unit.nva.indexing.handlers;

import static no.unit.nva.search.IndexingClient.objectMapper;
import static no.unit.nva.search.constants.ApplicationConstants.TICKETS_INDEX;
import static no.unit.nva.search.constants.ApplicationConstants.objectMapperWithEmpty;
import static no.unit.nva.search.models.IndexDocument.MISSING_IDENTIFIER_IN_RESOURCE;
import static no.unit.nva.search.models.IndexDocument.MISSING_INDEX_NAME_IN_RESOURCE;
import static no.unit.nva.testutils.RandomDataGenerator.randomJson;
import static no.unit.nva.testutils.RandomDataGenerator.randomString;
import static nva.commons.core.attempt.Try.attempt;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.stringContainsInOrder;
import static org.hamcrest.collection.IsIterableContainingInOrder.contains;
import static org.junit.jupiter.api.Assertions.assertThrows;
import com.amazonaws.services.lambda.runtime.Context;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.util.Set;
import no.unit.nva.events.models.AwsEventBridgeDetail;
import no.unit.nva.events.models.AwsEventBridgeEvent;
import no.unit.nva.events.models.EventReference;
import no.unit.nva.identifiers.SortableIdentifier;
import no.unit.nva.indexing.testutils.FakeIndexingClient;
import no.unit.nva.indexing.testutils.FakeSqsClient;
import no.unit.nva.s3.S3Driver;
import no.unit.nva.search.models.EventConsumptionAttributes;
import no.unit.nva.search.models.IndexDocument;
import no.unit.nva.stubs.FakeS3Client;
import no.unit.nva.testutils.RandomDataGenerator;
import nva.commons.core.paths.UnixPath;
import nva.commons.core.paths.UriWrapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import software.amazon.awssdk.services.s3.model.NoSuchKeyException;

public class IndexResourceHandlerTest {

    public static final String OPENSEARCH_RESOURCES_INDEX = "resources";
    public static final IndexDocument SAMPLE_RESOURCE = createSampleResource(SortableIdentifier.next(),
                                                                             OPENSEARCH_RESOURCES_INDEX);
    public static final IndexDocument SAMPLE_TICKET = createSampleResource(SortableIdentifier.next(),
                                                                             TICKETS_INDEX);
    public static final String FILE_DOES_NOT_EXIST = "File does not exist";
    public static final String IGNORED_TOPIC = "ignoredValue";
    private static final IndexDocument SAMPLE_RESOURCE_MISSING_IDENTIFIER =
        createSampleResource(null, OPENSEARCH_RESOURCES_INDEX);
    private static final IndexDocument SAMPLE_RESOURCE_MISSING_INDEX_NAME =
        createSampleResource(SortableIdentifier.next(), null);
    private S3Driver resourcesS3Driver;
    private IndexResourceHandler indexResourceHandler;
    private Context context;
    private ByteArrayOutputStream output;
    private FakeIndexingClient indexingClient;
    private FakeSqsClient sqsClient;


    @BeforeEach
    void init() {
        FakeS3Client fakeS3Client = new FakeS3Client();
        resourcesS3Driver = new S3Driver(fakeS3Client, "resources");
        indexingClient = new FakeIndexingClient();
        sqsClient = new FakeSqsClient();
        indexResourceHandler = new IndexResourceHandler(resourcesS3Driver, indexingClient, sqsClient);
        context = Mockito.mock(Context.class);
        output = new ByteArrayOutputStream();
    }

    @Test
    void shouldAddDocumentToIndexWhenResourceExistsInResourcesStorage() throws Exception {
        URI resourceLocation = prepareEventStorageResourceFile();

        InputStream input = createEventBridgeEvent(resourceLocation);
        indexResourceHandler.handleRequest(input, output, context);
        Set<JsonNode> allIndexedDocuments = indexingClient.listAllDocuments(SAMPLE_RESOURCE.getIndexName());
        assertThat(allIndexedDocuments, contains(SAMPLE_RESOURCE.getResource()));
    }

    @Test
    void shouldSendMessageToRecoveryQueueWhenIndexingResourceIsFailing() throws Exception {
        indexingClient = indexingClientThrowingException(randomString());
        indexResourceHandler = new IndexResourceHandler(resourcesS3Driver, indexingClient, sqsClient);
        var resourceLocation = prepareEventStorageResourceFile();
        var input = createEventBridgeEvent(resourceLocation);
        indexResourceHandler.handleRequest(input, output, context);

        var deliveredMessage = sqsClient.getDeliveredMessages().get(0);

        assertThat(deliveredMessage.messageAttributes().get("id").stringValue(), is(notNullValue()));
        assertThat(deliveredMessage.messageAttributes().get("type").stringValue(), is(equalTo("Resource")));
    }

    @Test
    void shouldSendMessageToRecoveryQueueWhenIndexingTicketIsFailing() throws Exception {
        indexingClient = indexingClientThrowingException(randomString());
        indexResourceHandler = new IndexResourceHandler(resourcesS3Driver, indexingClient, sqsClient);
        var resourceLocation = prepareEventStorageTicketFile();
        var input = createEventBridgeEvent(resourceLocation);
        indexResourceHandler.handleRequest(input, output, context);

        var deliveredMessage = sqsClient.getDeliveredMessages().get(0);

        assertThat(deliveredMessage.messageAttributes().get("id").stringValue(), is(notNullValue()));
        assertThat(deliveredMessage.messageAttributes().get("type").stringValue(), is(equalTo("Ticket")));
    }

    @Test
    void shouldThrowExceptionWhenResourceIsMissingIdentifier() throws Exception {
        URI resourceLocation = prepareEventStorageResourceFile(SAMPLE_RESOURCE_MISSING_IDENTIFIER);

        InputStream input = createEventBridgeEvent(resourceLocation);

        RuntimeException exception =
            assertThrows(RuntimeException.class, () -> indexResourceHandler.handleRequest(input, output, context));

        assertThat(exception.getMessage(), stringContainsInOrder(MISSING_IDENTIFIER_IN_RESOURCE));
    }

    @Test
    void shouldThrowNoSuchKeyExceptionWhenResourceIsMissingFromEventStorage() throws Exception {
        URI missingResourceLocation = RandomDataGenerator.randomUri();

        InputStream input = createEventBridgeEvent(missingResourceLocation);

        NoSuchKeyException exception = assertThrows(NoSuchKeyException.class,
                                                    () -> indexResourceHandler.handleRequest(input, output, context));

        assertThat(exception.getMessage(), stringContainsInOrder(FILE_DOES_NOT_EXIST));
    }

    @Test
    void shouldThrowExceptionWhenEventConsumptionAttributesIsMissingIndexName() throws Exception {
        URI resourceLocation = prepareEventStorageResourceFile(SAMPLE_RESOURCE_MISSING_INDEX_NAME);

        InputStream input = createEventBridgeEvent(resourceLocation);

        RuntimeException exception = assertThrows(RuntimeException.class,
                                                  () -> indexResourceHandler.handleRequest(input, output,
                                                                                           context));

        assertThat(exception.getMessage(), stringContainsInOrder(MISSING_INDEX_NAME_IN_RESOURCE));
    }

    private static IndexDocument createSampleResource(SortableIdentifier identifierProvider, String indexName) {
        String randomJson = randomJson();
        ObjectNode objectNode = attempt(() -> (ObjectNode) objectMapper.readTree(randomJson)).orElseThrow();
        EventConsumptionAttributes metadata = new EventConsumptionAttributes(indexName, identifierProvider);
        return new IndexDocument(metadata, objectNode);
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

    private URI prepareEventStorageTicketFile() throws IOException {
        return prepareEventStorageResourceFile(SAMPLE_TICKET);
    }

    private URI prepareEventStorageResourceFile(IndexDocument resource) throws IOException {
        URI resourceLocation = RandomDataGenerator.randomUri();
        UnixPath resourcePath = UriWrapper.fromUri(resourceLocation).toS3bucketPath();
        resourcesS3Driver.insertFile(resourcePath, resource.toJsonString());
        return resourceLocation;
    }

    private InputStream createEventBridgeEvent(URI resourceLocation) throws JsonProcessingException {
        EventReference indexResourceEvent = new EventReference(IGNORED_TOPIC, resourceLocation);

        AwsEventBridgeDetail<EventReference> detail = new AwsEventBridgeDetail<>();
        detail.setResponsePayload(indexResourceEvent);

        AwsEventBridgeEvent<AwsEventBridgeDetail<EventReference>> event = new AwsEventBridgeEvent<>();
        event.setDetail(detail);

        return new ByteArrayInputStream(objectMapperWithEmpty.writeValueAsBytes(event));
    }
}
