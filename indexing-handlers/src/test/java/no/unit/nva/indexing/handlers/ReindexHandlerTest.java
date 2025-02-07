package no.unit.nva.indexing.handlers;

import static no.unit.nva.LogAppender.getAppender;
import static no.unit.nva.testutils.RandomDataGenerator.randomString;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import no.unit.nva.indexing.model.ReindexRequest;
import no.unit.nva.indexingclient.IndexingClient;
import no.unit.nva.indexingclient.ReindexingException;
import no.unit.nva.stubs.FakeContext;
import nva.commons.core.ioutils.IoUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class ReindexHandlerTest {

  public static final FakeContext CONTEXT = new FakeContext();
  private IndexingClient indexingClient;
  private ReindexHandler reindexHandler;
  private ByteArrayOutputStream output;

  @BeforeEach
  void init() {
    indexingClient = mock(IndexingClient.class);
    reindexHandler = new ReindexHandler(indexingClient);
    output = new ByteArrayOutputStream();
  }

  @Test
  void shouldReindexOldIndexToNewIndex() throws IOException {
    var oldIndex = randomString();
    var newIndex = randomString();
    reindexHandler.handleRequest(request(oldIndex, newIndex), output, CONTEXT);

    verify(indexingClient, times(1)).reindex(eq(oldIndex), eq(newIndex), any());
  }

  @Test
  void shouldLogErrorMessageWhenReindexingFails() {
    doThrow(ReindexingException.class).when(indexingClient).reindex(any(), any(), any());
    var appender = getAppender(ReindexHandler.class);
    assertThrows(
        ReindexingException.class,
        () ->
            reindexHandler.handleRequest(request(randomString(), randomString()), output, CONTEXT));

    var message = appender.getEvents().getFirst().getMessage().getFormattedMessage();

    assertTrue(message.contains("Reindexing failed"));
  }

  private InputStream request(String oldIndex, String newIndex) {
    var request = new ReindexRequest(oldIndex, newIndex);
    return IoUtils.stringToStream(request.toJsonString());
  }
}
