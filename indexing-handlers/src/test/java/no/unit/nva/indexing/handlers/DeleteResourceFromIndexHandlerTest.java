package no.unit.nva.indexing.handlers;

import static no.unit.nva.LogAppender.getAppender;
import static no.unit.nva.LogAppender.logToString;
import static no.unit.nva.indexingclient.IndexingClient.objectMapper;
import static no.unit.nva.indexingclient.constants.ApplicationConstants.objectMapperWithEmpty;
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
import no.unit.nva.identifiers.SortableIdentifier;
import no.unit.nva.indexing.model.DeleteImportCandidateEvent;
import no.unit.nva.indexing.testutils.FakeIndexingClient;
import no.unit.nva.indexingclient.models.EventConsumptionAttributes;
import no.unit.nva.indexingclient.models.IndexDocument;
import org.apache.logging.log4j.core.test.appender.ListAppender;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

public class DeleteResourceFromIndexHandlerTest {

    public static final String RESOURCES_INDEX = "resource";

    public static final String SOMETHING_BAD_HAPPENED = "Something bad happened";
    private static final Context CONTEXT = Mockito.mock(Context.class);
    private FakeIndexingClient indexingClient;

    private ByteArrayOutputStream output;

    private DeleteResourceFromIndexHandler handler;

    private static ListAppender appender;

    @BeforeAll
    public static void initClass() {
        appender = getAppender(DeleteResourceFromIndexHandler.class);
    }

    @BeforeEach
    void init() {
        indexingClient = new FakeIndexingClient();
        handler = new DeleteResourceFromIndexHandler(indexingClient);
        output = new ByteArrayOutputStream();
    }

    @Test
    void shouldThrowRuntimeExceptionAndLogErrorWhenIndexingClientIsThrowingException() throws IOException {
//        final var appender = LogUtils.getTestingAppenderForRootLogger();
        indexingClient = new FakeIndexingClientThrowingException();
        handler = new DeleteResourceFromIndexHandler(indexingClient);
        try (var eventReference = createEventBridgeEvent(SortableIdentifier.next())) {
            assertThrows(RuntimeException.class,
                () -> handler.handleRequest(eventReference, output, CONTEXT));
        }
        assertThat(logToString(appender), containsString(SOMETHING_BAD_HAPPENED));
    }

    @Test
    void shouldRemoveDocumentFromSearchIndexClient() throws IOException {
        var resourceIdentifier = SortableIdentifier.next();
        var sampleDocument = createSampleResorce(resourceIdentifier);
        indexingClient.addDocumentToIndex(sampleDocument);
        var eventReference = createEventBridgeEvent(resourceIdentifier);
        handler.handleRequest(eventReference, output, CONTEXT);
        Set<JsonNode> allIndexedDocuments = indexingClient.listAllDocuments(RESOURCES_INDEX);
        assertThat(allIndexedDocuments, not(contains(sampleDocument.resource())));
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
        DeleteResourceEvent deleteResourceEvent = new DeleteResourceEvent(DeleteImportCandidateEvent.EVENT_TOPIC,
                                                                                 resourceIdentifier);

        AwsEventBridgeDetail<DeleteResourceEvent> detail = new AwsEventBridgeDetail<>();
        detail.setResponsePayload(deleteResourceEvent);

        AwsEventBridgeEvent<AwsEventBridgeDetail<DeleteResourceEvent>> event = new AwsEventBridgeEvent<>();
        event.setDetail(detail);

        return new ByteArrayInputStream(objectMapperWithEmpty.writeValueAsBytes(event));
    }

    static class FakeIndexingClientThrowingException extends FakeIndexingClient {

        @Override
        public void removeDocumentFromResourcesIndex(String identifier) throws IOException {
            throw new IOException(SOMETHING_BAD_HAPPENED);
        }
    }
}
