package no.unit.nva.indexing.handlers;

import static org.junit.jupiter.api.Assertions.assertThrows;
import com.amazonaws.services.lambda.runtime.Context;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import no.unit.nva.events.models.EventReference;
import no.unit.nva.identifiers.SortableIdentifier;
import no.unit.nva.indexing.testutils.FakeIndexingClient;
import no.unit.nva.s3.S3Driver;
import no.unit.nva.search.models.DeleteResourceEvent;
import no.unit.nva.stubs.FakeS3Client;
import no.unit.nva.testutils.EventBridgeEventBuilder;
import nva.commons.core.paths.UnixPath;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import software.amazon.awssdk.services.s3.S3Client;

public class DeleteResourceFromIndexHandlerTest {

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
    void shouldThrowRuntimeExceptionWhenIndexingClientIsThrowingException() throws IOException {
        indexingClient = new FakeIndexingClientThrowingException();
        handler = new DeleteResourceFromIndexHandler(indexingClient, s3Client);

        var eventBody = createEventBody();
        var eventReference = createEventReference(eventBody);
        var something = "";
        handler.handleRequest(eventReference, output, CONTEXT);
        assertThrows(RuntimeException.class, () -> handler.handleRequest(eventReference, output, CONTEXT));
    }

    private static DeleteResourceEvent createEventBody() {
        return new DeleteResourceEvent(DeleteResourceEvent.EVENT_TOPIC,
                                       SortableIdentifier.next(),
                                       null,
                                       null,
                                       null);
    }

    private InputStream createEventReference(DeleteResourceEvent eventBody) throws IOException {
        var eventFileUri = s3Driver.insertEvent(UnixPath.EMPTY_PATH, eventBody.toJsonString());
        var eventReference = new EventReference(DeleteResourceEvent.EVENT_TOPIC, null, eventFileUri);
        return EventBridgeEventBuilder.sampleEvent(eventReference);
    }

    class FakeIndexingClientThrowingException extends FakeIndexingClient {

        @Override
        public void removeDocumentFromIndex(String identifier) throws IOException {
            throw new IOException("Something bad happened");
        }
    }
}
