package no.unit.nva.indexingclient;

import static no.unit.nva.indexingclient.BatchIndexingConstants.config;
import static no.unit.nva.indexingclient.BatchIndexingConstants.defaultEventBridgeClient;
import static no.unit.nva.indexingclient.EmitEventUtils.emitEvent;
import static no.unit.nva.indexingclient.constants.ApplicationConstants.objectMapperWithEmpty;

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

    public static final String PERSISTED_RESOURCES_PATH =
            config.getString("batch.persistedResourcesPath");
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
