package no.unit.nva.indexing.utils;

import no.unit.nva.indexingclient.models.IndexDocument;
import no.unit.nva.indexingclient.models.QueueClient;

import nva.commons.core.Environment;

import software.amazon.awssdk.services.sqs.model.MessageAttributeValue;
import software.amazon.awssdk.services.sqs.model.SendMessageRequest;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Map;

public final class RecoveryEntry {

    public static final String DATA_TYPE_STRING = "String";
    public static final String RESOURCES_INDEX_NAME = "resources";
    public static final String RESOURCE = "Resource";
    public static final String TICKETS_INDEX_NAME = "tickets";
    public static final String UNSUPPORTED_DOCUMENT_MESSAGE = "Unsupported document!";
    public static final String TICKET = "Ticket";
    private static final String ID = "id";
    private static final String RECOVERY_QUEUE = "RECOVERY_QUEUE";
    private static final String TYPE = "type";
    private final String identifier;
    private final String type;
    private final String exception;

    private RecoveryEntry(String identifier, String type, String exception) {
        this.identifier = identifier;
        this.type = type;
        this.exception = exception;
    }

    public static RecoveryEntry fromIndexDocument(IndexDocument indexDocument) {
        return builder().withType(indexDocument.getType()).build();
    }

    private static Builder builder() {
        return new Builder();
    }

    public RecoveryEntry withIdentifier(String identifier) {
        return this.copy().withIdentifier(identifier).build();
    }

    public void persist(QueueClient queueClient) {
        queueClient.sendMessage(createSendMessageRequest());
    }

    private SendMessageRequest createSendMessageRequest() {
        return SendMessageRequest.builder()
                .messageAttributes(
                        Map.of(
                                ID, convertToMessageAttribute(identifier),
                                TYPE, convertToMessageAttribute(type)))
                .messageBody(exception)
                .queueUrl(new Environment().readEnv(RECOVERY_QUEUE))
                .build();
    }

    private MessageAttributeValue convertToMessageAttribute(String value) {
        return MessageAttributeValue.builder()
                .stringValue(value)
                .dataType(DATA_TYPE_STRING)
                .build();
    }

    public RecoveryEntry withException(Exception exception) {
        return this.copy().withException(getStackTrace(exception)).build();
    }

    public RecoveryEntry withIndex(String type) {
        return this.copy().withType(type).build();
    }

    private String getStackTrace(Exception exception) {
        var stringWriter = new StringWriter();
        exception.printStackTrace(new PrintWriter(stringWriter));
        return stringWriter.toString();
    }

    private Builder copy() {
        return new Builder()
                .withIdentifier(this.identifier)
                .withType(this.type)
                .withException(this.exception);
    }

    private static final class Builder {

        private String identifier;
        private String type;
        private String failure;

        private Builder() {}

        public Builder withIdentifier(String identifier) {
            this.identifier = identifier;
            return this;
        }

        public Builder withType(String type) {
            this.type = type;
            return this;
        }

        public Builder withException(String failure) {
            this.failure = failure;
            return this;
        }

        public RecoveryEntry build() {
            return new RecoveryEntry(identifier, type, failure);
        }
    }
}
