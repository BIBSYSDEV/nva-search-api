package no.sikt.nva.search.eventhandlers;

import static no.unit.nva.testutils.RandomDataGenerator.randomString;
import static no.unit.nva.testutils.RandomDataGenerator.randomUri;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.collection.IsIterableWithSize.iterableWithSize;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.amazonaws.services.lambda.runtime.events.SQSEvent;
import com.amazonaws.services.lambda.runtime.events.SQSEvent.SQSMessage;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import no.unit.nva.identifiers.SortableIdentifier;
import no.unit.nva.indexing.testutils.FakeIndexingClient;
import no.unit.nva.indexingclient.models.EventConsumptionAttributes;
import no.unit.nva.indexingclient.models.IndexDocument;
import no.unit.nva.stubs.FakeContext;
import no.unit.nva.stubs.FakeS3Client;
import nva.commons.core.Environment;
import nva.commons.core.paths.UriWrapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class ExternalUpdatesEventHandlerTest {
  private Environment environment;
  private FakeS3Client s3Client;

  @BeforeEach
  public void beforeEach() {
    environment = mock(Environment.class);
    when(environment.readEnv("EVENT_BUCKET_NAME")).thenReturn(randomString());
    when(environment.readEnv("SEARCH_INFRASTRUCTURE_AUTH_URI")).thenReturn("https://localhost");

    s3Client = new FakeS3Client();
  }

  @Test
  void shouldReturnWithoutIssuesIfNoMessagesInEvent() {

    var indexingClient = new FakeIndexingClient();

    var handler = new ExternalUpdatesEventHandler(environment, new FakeS3Client(), indexingClient);

    assertDoesNotThrow(() -> handler.handleRequest(new SQSEvent(), new FakeContext()));
  }

  @Test
  void shouldFailOnMessageFromUnknownTopic() {
    var messageBody =
        """
{
  "detail": {
    "responsePayload": {
      "topic": "DUMMY_TOPIC"
    }
  }
}
""";
    var indexingClient = new FakeIndexingClient();

    var handler = new ExternalUpdatesEventHandler(environment, new FakeS3Client(), indexingClient);

    var sqsMessage = new SQSMessage();
    sqsMessage.setBody(messageBody);

    var sqsEvent = new SQSEvent();
    sqsEvent.setRecords(List.of(sqsMessage));

    assertThrows(
        EventHandlingException.class, () -> handler.handleRequest(sqsEvent, new FakeContext()));
  }

  @Test
  void shouldFailOnUnknownActionInS3Event() {
    var uri = randomUri();
    var messageBody =
        String.format(
            """
{
  "detail": {
    "responsePayload": {
      "topic": "PublicationService.Resource.Deleted",
      "uri": "%s"
    }
  }
}
""",
            uri);
    var s3File =
        """
        {
          "action": "INSERT",
          "oldData": null,
          "newData": null
        }
        """;
    var filename = UriWrapper.fromUri(uri).getLastPathElement();
    var s3Client =
        FakeS3Client.fromContentsMap(
            Map.of(filename, new ByteArrayInputStream(s3File.getBytes(StandardCharsets.UTF_8))));
    var indexingClient = new FakeIndexingClient();
    var handler = new ExternalUpdatesEventHandler(environment, s3Client, indexingClient);

    var sqsMessage = new SQSMessage();
    sqsMessage.setBody(messageBody);

    var sqsEvent = new SQSEvent();
    sqsEvent.setRecords(List.of(sqsMessage));

    assertThrows(
        EventHandlingException.class, () -> handler.handleRequest(sqsEvent, new FakeContext()));
  }

  @Test
  void shouldFailWhenNotAbleToParseS3EventData() {
    var uri = randomUri();
    var messageBody =
        String.format(
            """
{
  "detail": {
    "responsePayload": {
      "topic": "PublicationService.Resource.Deleted",
      "uri": "%s"
    }
  }
}
""",
            uri);
    var s3File =
        """
        {
          "action": 1,
          "oldData": [],
          "newData": []
        }
        """;
    var filename = UriWrapper.fromUri(uri).getLastPathElement();
    var s3Client =
        FakeS3Client.fromContentsMap(
            Map.of(filename, new ByteArrayInputStream(s3File.getBytes(StandardCharsets.UTF_8))));
    var indexingClient = new FakeIndexingClient();
    var handler = new ExternalUpdatesEventHandler(environment, s3Client, indexingClient);

    var sqsMessage = new SQSMessage();
    sqsMessage.setBody(messageBody);

    var sqsEvent = new SQSEvent();
    sqsEvent.setRecords(List.of(sqsMessage));

    assertThrows(
        EventHandlingException.class, () -> handler.handleRequest(sqsEvent, new FakeContext()));
  }

  @Test
  void shouldFailWhenNotAbleToParseEventReference() {
    var uri = randomUri();
    var messageBody =
        String.format(
            """
{
  "detail": {
    "responsePayload": {
      "topic": 1,
      "uri": []
    }
  }
}
""",
            uri);
    var s3File =
        """
        {
          "action": "REMOVE",
          "oldData": {},
          "newData": null
        }
        """;
    var filename = UriWrapper.fromUri(uri).getLastPathElement();
    var s3Client =
        FakeS3Client.fromContentsMap(
            Map.of(filename, new ByteArrayInputStream(s3File.getBytes(StandardCharsets.UTF_8))));
    var indexingClient = new FakeIndexingClient();
    var handler = new ExternalUpdatesEventHandler(environment, s3Client, indexingClient);

    var sqsMessage = new SQSMessage();
    sqsMessage.setBody(messageBody);

    var sqsEvent = new SQSEvent();
    sqsEvent.setRecords(List.of(sqsMessage));

    assertThrows(
        EventHandlingException.class, () -> handler.handleRequest(sqsEvent, new FakeContext()));
  }

  @Test
  void shouldRemoveDocumentFromIndex() throws IOException {
    var uri = randomUri();
    var messageBody =
        String.format(
            """
{
  "detail": {
    "responsePayload": {
      "topic": "PublicationService.Resource.Deleted",
      "uri": "%s"
    }
  }
}
""",
            uri);

    var identifier = SortableIdentifier.next();
    var s3File =
        String.format(
            """
{
  "action": "REMOVE",
  "oldData": {
    "type": "Resource",
    "identifier": "%s"
  },
  "newData": null
}
""",
            identifier);
    var filename = UriWrapper.fromUri(uri).getLastPathElement();
    var s3Client =
        FakeS3Client.fromContentsMap(
            Map.of(filename, new ByteArrayInputStream(s3File.getBytes(StandardCharsets.UTF_8))));
    var indexingClient = new FakeIndexingClient();
    indexingClient.addDocumentToIndex(
        new IndexDocument(
            new EventConsumptionAttributes("resources", identifier),
            new ObjectNode(JsonNodeFactory.instance)));
    var handler = new ExternalUpdatesEventHandler(environment, s3Client, indexingClient);

    var sqsMessage = new SQSMessage();
    sqsMessage.setBody(messageBody);

    var sqsEvent = new SQSEvent();
    sqsEvent.setRecords(List.of(sqsMessage));

    assertDoesNotThrow(() -> handler.handleRequest(sqsEvent, new FakeContext()));

    assertThat(indexingClient.listAllDocuments("resources"), iterableWithSize(0));
  }

  @Test
  void shouldFailIfNotAbleToReachIndex() throws IOException {
    var uri = randomUri();
    var messageBody =
        String.format(
            """
{
  "detail": {
    "responsePayload": {
      "topic": "PublicationService.Resource.Deleted",
      "uri": "%s"
    }
  }
}
""",
            uri);

    var identifier = SortableIdentifier.next();
    var s3File =
        String.format(
            """
{
  "action": "REMOVE",
  "oldData": {
    "type": "Publication",
    "id": "https://example.com/abc",
    "identifier": "%s"
  },
  "newData": null
}
""",
            identifier);
    var filename = UriWrapper.fromUri(uri).getLastPathElement();
    var s3Client =
        FakeS3Client.fromContentsMap(
            Map.of(filename, new ByteArrayInputStream(s3File.getBytes(StandardCharsets.UTF_8))));
    var indexingClient = new FailingIndexingClient(new IOException("Always failing!"));
    var handler = new ExternalUpdatesEventHandler(environment, s3Client, indexingClient);

    var sqsMessage = new SQSMessage();
    sqsMessage.setBody(messageBody);

    var sqsEvent = new SQSEvent();
    sqsEvent.setRecords(List.of(sqsMessage));

    assertThrows(
        EventHandlingException.class, () -> handler.handleRequest(sqsEvent, new FakeContext()));
  }
}
