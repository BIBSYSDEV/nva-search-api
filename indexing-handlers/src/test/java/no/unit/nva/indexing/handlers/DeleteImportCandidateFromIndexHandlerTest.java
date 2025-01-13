package no.unit.nva.indexing.handlers;

import static no.unit.nva.search.model.constant.Defaults.objectMapperWithEmpty;
import static no.unit.nva.search.model.constant.Words.IMPORT_CANDIDATES_INDEX;
import static no.unit.nva.testutils.RandomDataGenerator.randomJson;
import static nva.commons.core.attempt.Try.attempt;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.collection.IsIterableContainingInOrder.contains;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.amazonaws.services.lambda.runtime.Context;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Set;
import no.unit.nva.events.models.AwsEventBridgeDetail;
import no.unit.nva.events.models.AwsEventBridgeEvent;
import no.unit.nva.identifiers.SortableIdentifier;
import no.unit.nva.indexing.model.DeleteImportCandidateEvent;
import no.unit.nva.indexing.testutils.FakeIndexingClient;
import no.unit.nva.search.model.records.EventConsumptionAttributes;
import no.unit.nva.search.model.records.IndexDocument;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

public class DeleteImportCandidateFromIndexHandlerTest {

  public static final String SOMETHING_BAD_HAPPENED = "Something bad happened";
  private static final Context CONTEXT = Mockito.mock(Context.class);
  private FakeIndexingClient indexingClient;

  private ByteArrayOutputStream output;

  private DeleteImportCandidateFromIndexHandler handler;

  private static IndexDocument createSampleResource(SortableIdentifier identifierProvider) {
    String randomJson = randomJson();
    ObjectNode objectNode =
        attempt(() -> (ObjectNode) objectMapperWithEmpty.readTree(randomJson)).orElseThrow();
    EventConsumptionAttributes metadata =
        new EventConsumptionAttributes(IMPORT_CANDIDATES_INDEX, identifierProvider);
    return new IndexDocument(metadata, objectNode);
  }

  @BeforeEach
  void init() {
    indexingClient = new FakeIndexingClient();
    handler = new DeleteImportCandidateFromIndexHandler(indexingClient);
    output = new ByteArrayOutputStream();
  }

  @Test
  void shouldThrowRuntimeExceptionWhenIndexingClientIsThrowingException() throws IOException {
    indexingClient =
        new DeleteImportCandidateFromIndexHandlerTest.FakeIndexingClientThrowingException();
    handler = new DeleteImportCandidateFromIndexHandler(indexingClient);
    try (var eventReference = createEventBridgeEvent(SortableIdentifier.next())) {
      assertThrows(
          RuntimeException.class, () -> handler.handleRequest(eventReference, output, CONTEXT));
    }
  }

  @Test
  void shouldRemoveDocumentFromSearchIndexClient() throws IOException {
    var resourceIdentifier = SortableIdentifier.next();
    var sampleDocument = createSampleResource(resourceIdentifier);
    indexingClient.addDocumentToIndex(sampleDocument);
    try (var eventReference = createEventBridgeEvent(resourceIdentifier)) {
      handler.handleRequest(eventReference, output, CONTEXT);
    }
    Set<JsonNode> allIndexedDocuments = indexingClient.listAllDocuments(IMPORT_CANDIDATES_INDEX);
    assertThat(allIndexedDocuments, not(contains(sampleDocument.resource())));
  }

  private InputStream createEventBridgeEvent(SortableIdentifier resourceIdentifier)
      throws IOException {
    DeleteResourceEvent deleteResourceEvent =
        new DeleteResourceEvent(DeleteImportCandidateEvent.EVENT_TOPIC, resourceIdentifier);

    AwsEventBridgeDetail<DeleteResourceEvent> detail = new AwsEventBridgeDetail<>();
    detail.setResponsePayload(deleteResourceEvent);

    AwsEventBridgeEvent<AwsEventBridgeDetail<DeleteResourceEvent>> event =
        new AwsEventBridgeEvent<>();
    event.setDetail(detail);

    return new ByteArrayInputStream(objectMapperWithEmpty.writeValueAsBytes(event));
  }

  static class FakeIndexingClientThrowingException extends FakeIndexingClient {

    @Override
    public void removeDocumentFromImportCandidateIndex(String identifier) throws IOException {
      throw new IOException(SOMETHING_BAD_HAPPENED);
    }
  }
}
