package no.unit.nva.indexingclient;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.Mockito.mock;

import com.amazonaws.services.lambda.runtime.events.SQSEvent;
import com.amazonaws.services.lambda.runtime.events.SQSEvent.SQSMessage;
import java.util.List;
import no.unit.nva.stubs.FakeContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class BatchIndexResourceHandlerTest {

  private BatchIndexResourceHandler handler;

  @BeforeEach
  void setUp() {
    var batchIndexService = mock(BatchIndexService.class);
    handler = new BatchIndexResourceHandler(batchIndexService);
  }

  @Test
  void shouldReturnWithoutIssuesIfNoMessagesInEvent() {
    assertDoesNotThrow(() -> handler.handleRequest(new SQSEvent(), new FakeContext()));
  }

  @Test
  void shouldProcessMessagesInEvent() {
    var sqsEvent = new SQSEvent();
    sqsEvent.setRecords(List.of(new SQSMessage()));

    assertDoesNotThrow(() -> handler.handleRequest(sqsEvent, new FakeContext()));
  }
}
