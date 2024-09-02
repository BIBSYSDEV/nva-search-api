package no.unit.nva.indexingclient;

import static no.unit.nva.indexingclient.StartBatchIndexingHandler.PERSISTED_RESOURCES_PATH;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsEqual.equalTo;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;

class StartBatchIndexingHandlerTest extends BatchIndexTest {

    private ByteArrayOutputStream outputStream;
    private StubEventBridgeClient eventBridgeClient;

    @BeforeEach
    public void initialize() {
        outputStream = new ByteArrayOutputStream();
        eventBridgeClient = new StubEventBridgeClient();
    }

    @Test
    void handlerSendsEventToEventBridgeWhenItReceivesAnImportRequest() throws IOException {

        var handler = newHandler();

        handler.handleRequest(newImportRequest(), outputStream, CONTEXT);
        var expectedImportRequest = new ImportDataRequestEvent(PERSISTED_RESOURCES_PATH);
        assertThat(eventBridgeClient.getLatestEvent(), is(equalTo(expectedImportRequest)));
    }

    private StartBatchIndexingHandler newHandler() {
        return new StartBatchIndexingHandler(eventBridgeClient);
    }

    private InputStream newImportRequest() {
        return null;
    }
}
