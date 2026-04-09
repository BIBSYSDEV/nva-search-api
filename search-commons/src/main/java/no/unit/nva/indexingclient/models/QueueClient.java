package no.unit.nva.indexingclient.models;

import software.amazon.awssdk.services.sqs.model.SendMessageRequest;

@FunctionalInterface
public interface QueueClient {

  void sendMessage(SendMessageRequest sendMessageRequest);
}
