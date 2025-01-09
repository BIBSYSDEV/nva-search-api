package no.unit.nva.indexing.testutils;

import java.util.ArrayList;
import java.util.List;
import no.unit.nva.indexingclient.models.QueueClient;
import software.amazon.awssdk.services.sqs.model.SendMessageRequest;

public class FakeSqsClient implements QueueClient {

  private final List<SendMessageRequest> deliveredMessages = new ArrayList<>();

  public List<SendMessageRequest> getDeliveredMessages() {
    return deliveredMessages;
  }

  @Override
  public void sendMessage(SendMessageRequest sendMessageRequest) {
    deliveredMessages.add(sendMessageRequest);
  }
}
