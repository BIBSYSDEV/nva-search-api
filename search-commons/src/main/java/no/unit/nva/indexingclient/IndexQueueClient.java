package no.unit.nva.indexingclient;

import java.time.Duration;
import no.unit.nva.indexingclient.models.QueueClient;
import nva.commons.core.Environment;
import nva.commons.core.JacocoGenerated;
import software.amazon.awssdk.http.SdkHttpClient;
import software.amazon.awssdk.http.apache.ApacheHttpClient;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.sqs.SqsClient;
import software.amazon.awssdk.services.sqs.model.SendMessageRequest;

@JacocoGenerated
public final class IndexQueueClient implements QueueClient {

    public static final String AWS_REGION = "AWS_REGION";
    private static final int MAX_CONNECTIONS = 10_000;
    private static final long IDLE_TIME = 30;
    private static final long TIMEOUT_TIME = 30;
    private final SqsClient sqsClient;

    private IndexQueueClient(SqsClient sqsClient) {
        this.sqsClient = sqsClient;
    }

    public static IndexQueueClient defaultQueueClient() {
        return new IndexQueueClient(defaultClient());
    }

    private static SqsClient defaultClient() {
        return SqsClient.builder()
                .region(getRegion())
                .httpClient(httpClientForConcurrentQueries())
                .build();
    }

    private static Region getRegion() {
        return new Environment().readEnvOpt(AWS_REGION).map(Region::of).orElse(Region.EU_WEST_1);
    }

    private static SdkHttpClient httpClientForConcurrentQueries() {
        return ApacheHttpClient.builder()
                .useIdleConnectionReaper(true)
                .maxConnections(MAX_CONNECTIONS)
                .connectionMaxIdleTime(Duration.ofSeconds(IDLE_TIME))
                .connectionTimeout(Duration.ofSeconds(TIMEOUT_TIME))
                .build();
    }

    @Override
    public void sendMessage(SendMessageRequest sendMessageRequest) {
        sqsClient.sendMessage(sendMessageRequest);
    }
}
