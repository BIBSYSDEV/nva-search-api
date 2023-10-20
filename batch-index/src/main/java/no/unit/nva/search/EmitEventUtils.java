package no.unit.nva.search;

import static no.unit.nva.search.BatchIndexingConstants.BATCH_INDEX_EVENT_BUS_NAME;
import com.amazonaws.services.lambda.runtime.Context;
import java.time.Instant;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.amazon.awssdk.services.eventbridge.EventBridgeClient;
import software.amazon.awssdk.services.eventbridge.model.PutEventsRequest;
import software.amazon.awssdk.services.eventbridge.model.PutEventsRequestEntry;
import software.amazon.awssdk.services.eventbridge.model.PutEventsResponse;

public final class EmitEventUtils {

    public static final String MANDATORY_UNUSED_SUBTOPIC = "DETAIL.WITH.TOPIC";
    private static Logger logger = LoggerFactory.getLogger(EmitEventUtils.class);

    private EmitEventUtils() {

    }

    public static void emitEvent(EventBridgeClient eventBridgeClient,
                                 ImportDataRequestEvent importDataRequest,
                                 Context context) {
        PutEventsRequestEntry putEventRequestEntry = eventEntry(importDataRequest, context);
        logger.info("BusName:" + BATCH_INDEX_EVENT_BUS_NAME);
        logger.info("Event:" + putEventRequestEntry.toString());
        PutEventsRequest putEventRequest = PutEventsRequest.builder().entries(putEventRequestEntry).build();
        PutEventsResponse response = eventBridgeClient.putEvents(putEventRequest);
        logger.info(response.toString());
    }

    private static PutEventsRequestEntry eventEntry(ImportDataRequestEvent importDataRequest, Context context) {
        return PutEventsRequestEntry.builder()
            .eventBusName(BATCH_INDEX_EVENT_BUS_NAME)
            .detailType(MANDATORY_UNUSED_SUBTOPIC)
            .source(EventBasedBatchIndexer.class.getName())
            .time(Instant.now())
            .detail(importDataRequest.toJsonString())
            .resources(context.getInvokedFunctionArn())
            .build();
    }
}
