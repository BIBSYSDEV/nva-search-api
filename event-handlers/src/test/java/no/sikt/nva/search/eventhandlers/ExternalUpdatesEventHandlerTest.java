package no.sikt.nva.search.eventhandlers;

import static no.unit.nva.testutils.RandomDataGenerator.randomString;
import static no.unit.nva.testutils.RandomDataGenerator.randomUri;
import static nva.commons.core.ioutils.IoUtils.stringFromResources;
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
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import no.unit.nva.identifiers.SortableIdentifier;
import no.unit.nva.indexing.testutils.FakeIndexingClient;
import no.unit.nva.indexingclient.IndexingClient;
import no.unit.nva.indexingclient.models.EventConsumptionAttributes;
import no.unit.nva.indexingclient.models.IndexDocument;
import no.unit.nva.stubs.FakeContext;
import no.unit.nva.stubs.FakeS3Client;
import nva.commons.core.Environment;
import nva.commons.core.paths.UriWrapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class ExternalUpdatesEventHandlerTest {

  private static final String MESSAGE_BODY_TEMPLATE =
      stringFromResources(Path.of("sqsMessageBodyTemplate.json"));
  private Environment environment;

  @BeforeEach
  public void beforeEach() {
    environment = mock(Environment.class);
    when(environment.readEnv("EVENT_BUCKET_NAME")).thenReturn(randomString());
  }

  @Test
  void shouldReturnWithoutIssuesIfNoMessagesInEvent() {

    var indexingClient = new FakeIndexingClient();

    var handler = new ExternalUpdatesEventHandler(environment, new FakeS3Client(), indexingClient);

    assertDoesNotThrow(() -> handler.handleRequest(new SQSEvent(), new FakeContext()));
  }

  @Test
  void shouldFailOnMessageFromUnknownTopic() {
    var s3Uri = randomUri();
    var messageBody = stringFromResources(Path.of("sqsMessageWithUnexpectedTopic.json"));
    var eventReference = "ignoreMe";
    var fixture = prepareForTesting(s3Uri, eventReference, messageBody, new FakeIndexingClient());

    assertThrows(
        EventHandlingException.class,
        () -> fixture.handler().handleRequest(fixture.sqsEvent(), new FakeContext()));
  }

  @Test
  void shouldFailOnUnknownActionInS3Event() {
    var s3Uri = randomUri();
    var messageBody = generateMessageBody(s3Uri);
    var eventReference = stringFromResources(Path.of("s3EventReferenceWithUnexpectedAction.json"));
    var fixture = prepareForTesting(s3Uri, eventReference, messageBody, new FakeIndexingClient());

    assertThrows(
        EventHandlingException.class,
        () -> fixture.handler().handleRequest(fixture.sqsEvent(), new FakeContext()));
  }

  @Test
  void shouldFailWhenNotAbleToParseS3EventData() {
    var s3Uri = randomUri();
    var messageBody = generateMessageBody(s3Uri);
    var unparsableS3EventReference =
        stringFromResources(Path.of("unparsableS3EventReference.json"));
    var fixture =
        prepareForTesting(s3Uri, unparsableS3EventReference, messageBody, new FakeIndexingClient());

    assertThrows(
        EventHandlingException.class,
        () -> fixture.handler().handleRequest(fixture.sqsEvent(), new FakeContext()));
  }

  @Test
  void shouldFailWhenNotAbleToParseEventReference() {
    var s3Uri = randomUri();
    var invalidMessageBody = stringFromResources(Path.of("unparsableSqsMessageBody.json"));
    var eventReference = "ignoreMe";
    var fixture =
        prepareForTesting(s3Uri, eventReference, invalidMessageBody, new FakeIndexingClient());

    assertThrows(
        EventHandlingException.class,
        () -> fixture.handler().handleRequest(fixture.sqsEvent(), new FakeContext()));
  }

  @Test
  void shouldRemoveDocumentFromIndex() throws IOException {
    var s3Uri = randomUri();
    var messageBody = generateMessageBody(s3Uri);

    var identifier = SortableIdentifier.next();
    var eventReference = generateEventReference(identifier);

    var indexingClient = new FakeIndexingClient();
    indexingClient.addDocumentToIndex(
        new IndexDocument(
            new EventConsumptionAttributes("resources", identifier),
            new ObjectNode(JsonNodeFactory.instance)));

    var fixture = prepareForTesting(s3Uri, eventReference, messageBody, indexingClient);

    assertDoesNotThrow(
        () -> fixture.handler().handleRequest(fixture.sqsEvent(), new FakeContext()));

    assertThat(indexingClient.listAllDocuments("resources"), iterableWithSize(0));
  }

  @Test
  void shouldFailIfNotAbleToReachIndex() {
    var uri = randomUri();
    var messageBody = generateMessageBody(uri);

    var identifier = SortableIdentifier.next();
    var eventReference = generateEventReference(identifier);

    var indexingClient = new FailingIndexingClient(new IOException("Always failing!"));
    var fixture = prepareForTesting(uri, eventReference, messageBody, indexingClient);

    assertThrows(
        EventHandlingException.class,
        () -> fixture.handler().handleRequest(fixture.sqsEvent(), new FakeContext()));
  }

  private String generateEventReference(SortableIdentifier identifier) {
    var eventReferenceTemplate = stringFromResources(Path.of("eventReferenceTemplate.json"));
    return String.format(eventReferenceTemplate, identifier);
  }

  private static String generateMessageBody(URI uri) {
    return String.format(MESSAGE_BODY_TEMPLATE, uri);
  }

  private Fixture prepareForTesting(
      URI uri, String eventReference, String invalidMessageBody, IndexingClient indexingClient) {
    var filename = UriWrapper.fromUri(uri).getLastPathElement();
    var s3Client =
        FakeS3Client.fromContentsMap(
            Map.of(
                filename,
                new ByteArrayInputStream(eventReference.getBytes(StandardCharsets.UTF_8))));
    var handler = new ExternalUpdatesEventHandler(environment, s3Client, indexingClient);

    var sqsMessage = new SQSMessage();
    sqsMessage.setBody(invalidMessageBody);

    var sqsEvent = new SQSEvent();
    sqsEvent.setRecords(List.of(sqsMessage));
    return new Fixture(handler, sqsEvent);
  }

  private record Fixture(ExternalUpdatesEventHandler handler, SQSEvent sqsEvent) {}
}
