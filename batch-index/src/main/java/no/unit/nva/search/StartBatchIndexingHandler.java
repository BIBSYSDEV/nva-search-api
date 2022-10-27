package no.unit.nva.search;

import static no.unit.nva.search.BatchIndexingConstants.defaultEventBridgeClient;
import static no.unit.nva.search.EmitEventUtils.emitEvent;
import static no.unit.nva.search.constants.ApplicationConstants.objectMapperWithEmpty;
import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestStreamHandler;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import nva.commons.core.JacocoGenerated;
import software.amazon.awssdk.services.eventbridge.EventBridgeClient;


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
    public void handleRequest(InputStream input, OutputStream output, Context context) throws IOException {
        var firstImportRequestEvent = new ImportDataRequestEvent(BatchIndexingConstants.PERSISTED_RESOURCES_PATH);
        emitEvent(eventBridgeClient, firstImportRequestEvent, context);
        writeOutput(output);
    }

    protected void writeOutput(OutputStream outputStream)
        throws IOException {
        try (BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(outputStream))) {
            String outputJson = objectMapperWithEmpty.writeValueAsString("OK");
            writer.write(outputJson);
        }
    }

    
}
