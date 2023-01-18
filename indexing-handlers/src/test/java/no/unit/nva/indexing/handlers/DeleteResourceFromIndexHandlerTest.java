package no.unit.nva.indexing.handlers;

import static no.unit.nva.search.IndexingClient.objectMapper;
import static no.unit.nva.search.constants.ApplicationConstants.objectMapperWithEmpty;
import static no.unit.nva.testutils.RandomDataGenerator.randomJson;
import static nva.commons.core.attempt.Try.attempt;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.collection.IsIterableContainingInOrder.contains;
import static org.junit.jupiter.api.Assertions.assertThrows;
import com.amazonaws.services.lambda.runtime.Context;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Set;
import no.unit.nva.events.models.AwsEventBridgeDetail;
import no.unit.nva.events.models.AwsEventBridgeEvent;
import no.unit.nva.events.models.EventReference;
import no.unit.nva.identifiers.SortableIdentifier;
import no.unit.nva.indexing.testutils.FakeIndexingClient;
import no.unit.nva.s3.S3Driver;
import no.unit.nva.search.models.EventConsumptionAttributes;
import no.unit.nva.search.models.IndexDocument;
import no.unit.nva.stubs.FakeS3Client;
import nva.commons.core.paths.UnixPath;
import nva.commons.logutils.LogUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import software.amazon.awssdk.services.s3.S3Client;

public class DeleteResourceFromIndexHandlerTest {

    public static final String RESOURCES_INDEX = "resource";

    public static final String SOMETHING_BAD_HAPPENED = "Something bad happened";
    private final Context CONTEXT = Mockito.mock(Context.class);
    private FakeIndexingClient indexingClient;
    private S3Client s3Client;
    private S3Driver s3Driver;

    private ByteArrayOutputStream output;

    private DeleteResourceFromIndexHandler handler;

    @BeforeEach
    void init() {
        s3Client = new FakeS3Client();
        s3Driver = new S3Driver(s3Client, "ignored");
        indexingClient = new FakeIndexingClient();
        handler = new DeleteResourceFromIndexHandler(indexingClient, s3Client);
        output = new ByteArrayOutputStream();
    }

    @Test
    void shouldThrowRuntimeExceptionAndLogErrorWhenIndexingClientIsThrowingException() throws IOException {
        var appender = LogUtils.getTestingAppenderForRootLogger();
        indexingClient = new FakeIndexingClientThrowingException();
        handler = new DeleteResourceFromIndexHandler(indexingClient, s3Client);
        var eventReference = createEventBridgeEvent(SortableIdentifier.next());
        assertThrows(RuntimeException.class,
                     () -> handler.handleRequest(eventReference, output, CONTEXT));
        assertThat(appender.getMessages(), containsString(SOMETHING_BAD_HAPPENED));
    }

    @Test
    void shouldRemoveDocumentFromSearchIndexClient() throws IOException {
        var resourceIdentifier = SortableIdentifier.next();
        var sampleDocument = createSampleResorce(resourceIdentifier);
        indexingClient.addDocumentToIndex(sampleDocument);
        var eventReference = createEventBridgeEvent(resourceIdentifier);
        handler.handleRequest(eventReference, output, CONTEXT);
        Set<JsonNode> allIndexedDocuments = indexingClient.listAllDocuments(RESOURCES_INDEX);
        assertThat(allIndexedDocuments, not(contains(sampleDocument.getResource())));
    }

    private static DeleteResourceEvent createEventBody(SortableIdentifier resourceIdentifier) {
        return new DeleteResourceEvent(DeleteResourceEvent.EVENT_TOPIC,
                                       resourceIdentifier,
                                       null,
                                       null,
                                       null);
    }

    private static IndexDocument createSampleResource(SortableIdentifier identifierProvider) {
        String randomJson = randomJson();
        ObjectNode objectNode = attempt(() -> (ObjectNode) objectMapper.readTree(randomJson)).orElseThrow();
        EventConsumptionAttributes metadata = new EventConsumptionAttributes(RESOURCES_INDEX, identifierProvider);
        return new IndexDocument(metadata, objectNode);
    }

    private IndexDocument createSampleResorce(SortableIdentifier resourceIdentifier) {
        return createSampleResource(resourceIdentifier);
    }

    private InputStream createEventBridgeEvent(SortableIdentifier resourceIdentifier) throws IOException {
        var eventBody = createEventBody(resourceIdentifier);
        var eventFileUri = s3Driver.insertEvent(UnixPath.EMPTY_PATH, eventBody.toJsonString());
        EventReference eventReference = new EventReference(DeleteResourceEvent.EVENT_TOPIC, eventFileUri);

        AwsEventBridgeDetail<EventReference> detail = new AwsEventBridgeDetail<>();
        detail.setResponsePayload(eventReference);

        AwsEventBridgeEvent<AwsEventBridgeDetail<EventReference>> event = new AwsEventBridgeEvent<>();
        event.setDetail(detail);

        return new ByteArrayInputStream(objectMapperWithEmpty.writeValueAsBytes(event));
    }

    static class FakeIndexingClientThrowingException extends FakeIndexingClient {

        @Override
        public void removeDocumentFromIndex(String identifier) throws IOException {
            throw new IOException(SOMETHING_BAD_HAPPENED);
        }
    }
}
