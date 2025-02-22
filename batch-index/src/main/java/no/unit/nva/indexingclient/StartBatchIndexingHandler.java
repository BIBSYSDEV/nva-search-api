package no.unit.nva.indexingclient;

import static no.unit.nva.constants.Defaults.objectMapperWithEmpty;
import static no.unit.nva.indexingclient.Constants.PERSISTED_RESOURCES_PATH;
import static no.unit.nva.indexingclient.Constants.defaultEventBridgeClient;
import static no.unit.nva.indexingclient.EmitEventUtils.emitEvent;

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
