package no.unit.nva.indexing.handlers;

import static no.unit.nva.constants.IndexMappingsAndSettings.IMPORT_CANDIDATE_MAPPINGS;
import static no.unit.nva.constants.IndexMappingsAndSettings.RESOURCE_MAPPINGS;
import static no.unit.nva.constants.IndexMappingsAndSettings.RESOURCE_SETTINGS;
import static no.unit.nva.constants.IndexMappingsAndSettings.TICKET_MAPPINGS;
import static no.unit.nva.constants.Words.IMPORT_CANDIDATES_INDEX;
import static no.unit.nva.indexing.handlers.IndexName.IMPORT_CANDIDATES;
import static no.unit.nva.indexing.handlers.IndexName.RESOURCES;
import static no.unit.nva.indexing.handlers.IndexName.TICKETS;
import static no.unit.nva.testutils.RandomDataGenerator.randomString;
import static nva.commons.core.attempt.Try.attempt;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.amazonaws.services.lambda.runtime.Context;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import no.unit.nva.constants.Words;
import no.unit.nva.indexingclient.IndexingClient;
import nva.commons.core.ioutils.IoUtils;
import nva.commons.logutils.LogUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

class InitHandlerTest {

  private static final Map<String, IndexRequest> indices =
      Map.of(
          Words.RESOURCES,
          new IndexRequest(Words.RESOURCES, RESOURCE_MAPPINGS.asJson(), RESOURCE_SETTINGS.asJson()),
          Words.TICKETS,
          new IndexRequest(Words.TICKETS, TICKET_MAPPINGS.asJson()),
          IMPORT_CANDIDATES_INDEX,
          new IndexRequest(IMPORT_CANDIDATES_INDEX, IMPORT_CANDIDATE_MAPPINGS.asJson()));
  private ByteArrayOutputStream output;
  private InitHandler initHandler;
  private IndexingClient indexingClient;
  private Context context;

  @BeforeEach
  void init() {
    indexingClient = mock(IndexingClient.class);
    initHandler = new InitHandler(indexingClient);
    context = mock(Context.class);
    output = new ByteArrayOutputStream();
  }

  @Test
  void shouldNotThrowExceptionIfIndicesClientDoesNotThrowException() throws IOException {
    var appender = LogUtils.getTestingAppender(InitHandler.class);
    doNothing().when(indexingClient).createIndex(any(String.class));
    initHandler.handleRequest(createRequest(List.of(RESOURCES)), output, context);

    assertFalse(appender.getMessages().contains("error"));
  }

  @Test
  void shouldCreateIndicesProvidedInRequestOnly() throws IOException {
    var indicesToCreate = List.of(RESOURCES, TICKETS);
    mockIndicesCreation(indicesToCreate);

    initHandler.handleRequest(createRequest(indicesToCreate), output, context);

    verifyIndexCreation(RESOURCES);
    verifyIndexCreation(TICKETS);
  }

  @Test
  void shouldLogWarningWhenIndexingClientFailedToCreateIndex() throws IOException {
    var appender = LogUtils.getTestingAppender(InitHandler.class);
    var expectedMessage = randomString();
    when(indexingClient.createIndex(Mockito.anyString(), Mockito.anyMap(), Mockito.anyMap()))
        .thenThrow(new IOException(expectedMessage));
    initHandler.handleRequest(createRequest(List.of(RESOURCES)), output, context);

    assertTrue(appender.getMessages().contains(expectedMessage));
  }

  @Test
  void shouldCreateAllIndicesWhenEmptyListProvidedInRequest() throws IOException {
    var indicesToCreate = Arrays.asList(IndexName.values());
    mockIndicesCreation(indicesToCreate);

    initHandler.handleRequest(createRequest(List.of()), output, context);

    verifyIndexCreation(RESOURCES);
    verifyIndexCreation(TICKETS);
    verifyIndexCreation(IMPORT_CANDIDATES);
  }

  @Test
  void shouldLogWarningAndReturnFailedWhenRequestIsNotParsable() {
    var throwable =
        assertThrows(
            IllegalArgumentException.class,
            () -> initHandler.handleRequest(InputStream.nullInputStream(), output, context));
    assertEquals("Could not parse request!", throwable.getMessage());
  }

  private static InputStream createRequest(List<IndexName> indicesToCreate) {
    return IoUtils.stringToStream(new CreateIndexRequest(indicesToCreate).toJsonString());
  }

  private void verifyIndexCreation(IndexName indexName) throws IOException {
    var indexNameValue = indexName.getValue();
    verify(indexingClient)
        .createIndex(
            indices.get(indexNameValue).name(),
            indices.get(indexNameValue).mappings(),
            indices.get(indexNameValue).settings());
  }

  private void mockIndicesCreation(List<IndexName> indexNames) {
    indexNames.stream()
        .map(indexName -> indices.get(indexName.getValue()))
        .forEach(this::mockCreateIndexRequest);
  }

  private void mockCreateIndexRequest(IndexRequest indexRequest) {
    attempt(
        () ->
            doNothing()
                .when(indexingClient)
                .createIndex(
                    indexRequest.name(), indexRequest.mappings(), indexRequest.settings()));
  }
}
