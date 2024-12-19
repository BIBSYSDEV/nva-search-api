package no.unit.nva.indexing.batch;

import static no.unit.nva.indexing.batch.Constants.BATCH_INDEX_EVENT_BUS_NAME;
import static no.unit.nva.indexing.batch.Constants.MANDATORY_UNUSED_SUBTOPIC;

import com.amazonaws.services.lambda.runtime.Context;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import software.amazon.awssdk.services.eventbridge.EventBridgeClient;
import software.amazon.awssdk.services.eventbridge.model.PutEventsRequest;
import software.amazon.awssdk.services.eventbridge.model.PutEventsRequestEntry;

import java.time.Instant;

public final class EmitEventUtils {

    private static final Logger logger = LoggerFactory.getLogger(EmitEventUtils.class);

    private EmitEventUtils() {}

    public static void emitEvent(
            EventBridgeClient eventBridgeClient,
            ImportDataRequestEvent importDataRequest,
            Context context) {
        var putEventRequestEntry = eventEntry(importDataRequest, context);
        logger.debug("BusName:" + BATCH_INDEX_EVENT_BUS_NAME);
        logger.debug("Event:" + putEventRequestEntry.toString());
        var putEventRequest = PutEventsRequest.builder().entries(putEventRequestEntry).build();
        var response = eventBridgeClient.putEvents(putEventRequest);
        logger.debug(response.toString());
    }

    private static PutEventsRequestEntry eventEntry(
            ImportDataRequestEvent importDataRequest, Context context) {
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
