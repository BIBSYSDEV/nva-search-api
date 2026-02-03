package no.unit.nva.indexing.handlers;

import static no.unit.nva.indexing.handlers.IndexName.RESOURCES;
import static no.unit.nva.testutils.RandomDataGenerator.randomString;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.amazonaws.services.lambda.runtime.Context;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Collections;
import java.util.List;
import no.unit.nva.indexingclient.IndexingClient;
import nva.commons.core.ioutils.IoUtils;
import nva.commons.logutils.LogUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

class DeleteIndicesHandlerTest {

  private ByteArrayOutputStream output;
  private DeleteIndicesHandler handler;
  private IndexingClient indexingClient;
  private Context context;

  @BeforeEach
  void init() {
    indexingClient = mock(IndexingClient.class);
    handler = new DeleteIndicesHandler(indexingClient);
    context = mock(Context.class);
    output = new ByteArrayOutputStream();
  }

  @Test
  void shouldDeleteIndicesWhenFunctionIsInvoked() throws IOException {
    doNothing().when(indexingClient).deleteIndex(any(String.class));
    assertDoesNotThrow(
        () -> handler.handleRequest(createRequest(List.of(RESOURCES)), output, context));
  }

  @Test
  void shouldLogWarningWhenIndexDeletionFails() throws IOException {
    var appender = LogUtils.getTestingAppender(DeleteIndicesHandler.class);
    var expectedMessage = clientThrowingExceptionWithMessage();
    handler.handleRequest(createRequest(List.of(RESOURCES)), output, context);

    assertTrue(appender.getMessages().contains(expectedMessage));
  }

  @Test
  void shouldThrowExceptionWhenInputIsEmpty() {
    var throwable =
        assertThrows(
            IllegalStateException.class,
            () -> handler.handleRequest(createRequest(Collections.emptyList()), output, context));

    assertTrue(throwable.getMessage().contains("Provide at least one index to delete!"));
  }

  @Test
  void shouldThrowExceptionWhenUnknownIndexProvidedInInput() {
    var throwable =
        assertThrows(
            IllegalArgumentException.class,
            () -> handler.handleRequest(requestWithIndex(randomString()), output, context));

    assertTrue(throwable.getMessage().contains("Could not parse request!"));
  }

  private static InputStream requestWithIndex(String index) {
    return IoUtils.stringToStream(
        """
        {
        "indices": [ "%s" ]
        }
        """
            .formatted(index));
  }

  private String clientThrowingExceptionWithMessage() throws IOException {
    var expectedMessage = randomString();
    when(indexingClient.deleteIndex(Mockito.anyString()))
        .thenThrow(new IOException(expectedMessage));
    return expectedMessage;
  }

  private static InputStream createRequest(List<IndexName> indices) {
    return IoUtils.stringToStream(new DeleteIndicesRequest(indices).toJsonString());
  }
}
