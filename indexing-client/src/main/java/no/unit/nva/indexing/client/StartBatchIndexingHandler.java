package no.unit.nva.indexing.client;

import static no.unit.nva.indexing.client.Constants.PERSISTED_RESOURCES_PATH;
import static no.unit.nva.indexing.client.Constants.defaultEventBridgeClient;
import static no.unit.nva.indexing.client.EmitEventUtils.emitEvent;
import static no.unit.nva.search.model.constant.Defaults.objectMapperWithEmpty;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestStreamHandler;

import nva.commons.core.JacocoGenerated;

import software.amazon.awssdk.services.eventbridge.EventBridgeClient;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;

public class StartBatchIndexingHandler implements RequestStreamHandler {

    private final EventBridgeClient eventBridgeClient;

    @JacocoGenerated
    public StartBatchIndexingHandler() {
        this(defaultEventBridgeClient());
    }

    public StartBatchIndexingHandler(EventBridgeClient eventBridgeClient) {
        this.eventBridgeClient = eventBridgeClient;
    }

    @Override
    public void handleRequest(InputStream input, OutputStream output, Context context)
            throws IOException {
        var firstImportRequestEvent = new ImportDataRequestEvent(PERSISTED_RESOURCES_PATH);
        emitEvent(eventBridgeClient, firstImportRequestEvent, context);
        writeOutput(output);
    }

    protected void writeOutput(OutputStream outputStream) throws IOException {
        try (BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(outputStream))) {
            String outputJson = objectMapperWithEmpty.writeValueAsString("OK");
            writer.write(outputJson);
        }
    }
}
