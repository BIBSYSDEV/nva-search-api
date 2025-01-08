package no.unit.nva.indexing.handlers;

import static no.unit.nva.LogAppender.getAppender;
import static no.unit.nva.LogAppender.logToString;
import static no.unit.nva.indexing.handlers.InitHandler.FAILED;
import static no.unit.nva.indexing.handlers.InitHandler.SUCCESS;
import static no.unit.nva.testutils.RandomDataGenerator.randomString;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.StringContains.containsString;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.amazonaws.services.lambda.runtime.Context;

import no.unit.nva.indexingclient.IndexingClient;

import org.apache.logging.log4j.core.test.appender.ListAppender;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.io.IOException;

class InitHandlerTest {

  private static ListAppender appender;
  private InitHandler initHandler;
  private IndexingClient indexingClient;
  private Context context;

  @BeforeAll
  public static void initClass() {
    appender = getAppender(InitHandler.class);
  }

  @BeforeEach
  void init() {
    indexingClient = mock(IndexingClient.class);
    initHandler = new InitHandler(indexingClient);
    context = mock(Context.class);
  }

  @Test
  void shouldNotThrowExceptionIfIndicesClientDoesNotThrowException() throws IOException {
    doNothing().when(indexingClient).createIndex(any(String.class));
    var response = initHandler.handleRequest(null, context);
    assertEquals(SUCCESS, response);
  }

  @Test
  void shouldLogWarningAndReturnFailedWhenIndexingClientFailedToCreateIndex() throws IOException {
    String expectedMessage = randomString();
    when(indexingClient.createIndex(Mockito.anyString(), Mockito.anyMap(), Mockito.anyMap()))
        .thenThrow(new IOException(expectedMessage));
    var response = initHandler.handleRequest(null, context);
    assertEquals(FAILED, response);

    assertThat(logToString(appender), containsString(expectedMessage));
  }
}
