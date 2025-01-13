package no.unit.nva.indexing.handlers;

import static no.unit.nva.LogAppender.getAppender;
import static no.unit.nva.LogAppender.logToString;
import static no.unit.nva.search.model.constant.Words.IMPORT_CANDIDATES_INDEX;
import static no.unit.nva.testutils.RandomDataGenerator.randomString;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.core.StringContains.containsString;

import java.util.ArrayList;
import no.unit.nva.search.model.IndexingClient;
import no.unit.nva.stubs.FakeContext;
import org.apache.logging.log4j.core.test.appender.ListAppender;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

public class DeleteImportCandidateIndexHandlerTest {

  private static ListAppender appender;

  @BeforeAll
  public static void initClass() {
    appender = getAppender(DeleteIndicesHandler.class);
  }

  @Test
  void shouldDeleteIndicesWhenFunctionIsInvoked() {
    final var buffer = new ArrayList<String>();
    var indexingClient =
        new IndexingClient(null, null) {
          @Override
          public Void deleteIndex(String indexName) {
            buffer.add(indexName);
            return null;
          }
        };
    var handler = new DeleteImportCandidateIndexHandler(indexingClient);
    handler.handleRequest(null, new FakeContext());
    assertThat(buffer, hasItem(IMPORT_CANDIDATES_INDEX));
  }

  @Test
  void shouldLogWarningWhenIndexDeletionFails() {
    var expectedMessage = randomString();
    var indexingClient =
        new IndexingClient(null, null) {
          @Override
          public Void deleteIndex(String indexName) {
            throw new RuntimeException(expectedMessage);
          }
        };
    var handler = new DeleteImportCandidateIndexHandler(indexingClient);
    handler.handleRequest(null, new FakeContext());
    assertThat(logToString(appender), containsString(expectedMessage));
  }
}
