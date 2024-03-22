package no.unit.nva.search.utils;

import software.amazon.awssdk.services.sqs.model.SendMessageRequest;

public interface QueueClient {

    void sendMessage(SendMessageRequest sendMessageRequest);

}