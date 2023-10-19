package no.unit.nva.search.keybatch;

import static no.unit.nva.search.EmitEventUtils.INDICATION_THAT_EVENT_TYPE_IS_INSIDE_DETAIL;
import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestStreamHandler;
import java.io.InputStream;
import java.io.OutputStream;
import java.time.Instant;
import no.unit.nva.search.EventBasedBatchIndexer;
import nva.commons.core.Environment;
import nva.commons.core.JacocoGenerated;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.amazon.awssdk.http.urlconnection.UrlConnectionHttpClient;
import software.amazon.awssdk.services.eventbridge.EventBridgeClient;
import software.amazon.awssdk.services.eventbridge.model.PutEventsRequest;
import software.amazon.awssdk.services.eventbridge.model.PutEventsRequestEntry;
import software.amazon.awssdk.services.eventbridge.model.PutEventsResponse;

public class StartKeyBasedBatchHandler implements RequestStreamHandler {

    public static final String EVENT_BUS = new Environment().readEnv("EVENT_BUS");
    public static final String TOPIC = new Environment().readEnv("TOPIC");
    private static final Logger logger = LoggerFactory.getLogger(StartKeyBasedBatchHandler.class);
    private final EventBridgeClient client;

    @JacocoGenerated
    public StartKeyBasedBatchHandler() {
        this(defaultClient());
    }

    public StartKeyBasedBatchHandler(EventBridgeClient client) {
        this.client = client;
    }

    @Override
    public void handleRequest(InputStream input, OutputStream output, Context context) {
        var event = constructRequestEntry(context);
        var response = sendEvent(event);
        logger.info(response.toString());
    }

    private static PutEventsRequestEntry constructRequestEntry(Context context) {
        return PutEventsRequestEntry.builder()
                   .eventBusName(EVENT_BUS)
                   .detail(new KeyBatchRequestEvent(null, TOPIC).toJsonString())
                   .detailType(INDICATION_THAT_EVENT_TYPE_IS_INSIDE_DETAIL)
                   .source(EventBasedBatchIndexer.class.getName())
                   .resources(context.getInvokedFunctionArn())
                   .time(Instant.now())
                   .build();
    }

    @JacocoGenerated
    private static EventBridgeClient defaultClient() {
        return EventBridgeClient.builder().httpClientBuilder(UrlConnectionHttpClient.builder()).build();
    }

    private PutEventsResponse sendEvent(PutEventsRequestEntry event) {
        PutEventsRequest putEventsRequest = PutEventsRequest.builder().entries(event).build();
        return client.putEvents(putEventsRequest);
    }
}