package no.unit.nva.indexing.utils;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Map;
import no.unit.nva.search.models.IndexDocument;
import no.unit.nva.search.utils.QueueClient;
import nva.commons.core.Environment;
import software.amazon.awssdk.services.sqs.model.MessageAttributeValue;
import software.amazon.awssdk.services.sqs.model.SendMessageRequest;

public final class RecoveryEntry {

    private static final String ID = "id";
    private static final String RECOVERY_QUEUE = "RECOVERY_QUEUE";
    private static final String TYPE = "type";
    public static final String DATA_TYPE_STRING = "String";
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

    public RecoveryEntry withIdentifier(String identifier) {
        return this.copy().withIdentifier(identifier).build();
    }

    public void persist(QueueClient queueClient) {
        queueClient.sendMessage(createSendMessageRequest());
    }

    private SendMessageRequest createSendMessageRequest() {
        return SendMessageRequest.builder()
                   .messageAttributes(Map.of(ID, convertToMessageAttribute(identifier),
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

    private String getStackTrace(Exception exception) {
        var stringWriter = new StringWriter();
        exception.printStackTrace(new PrintWriter(stringWriter));
        return stringWriter.toString();
    }

    private static Builder builder() {
        return new Builder();
    }

    private Builder copy() {
        return new Builder().withIdentifier(this.identifier).withType(this.type).withException(this.exception);
    }

    private static final class Builder {

        private String identifier;
        private String type;
        private String failure;

        private Builder() {
        }

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
            return new RecoveryEntry(identifier, type,failure);
        }
    }
}
