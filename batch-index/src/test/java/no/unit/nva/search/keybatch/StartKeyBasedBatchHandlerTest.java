package no.unit.nva.search.keybatch;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.core.Is.is;
import static org.mockito.Mockito.mock;
import com.amazonaws.services.lambda.runtime.Context;
import com.fasterxml.jackson.core.JsonProcessingException;
import no.unit.nva.commons.json.JsonUtils;
import no.unit.nva.stubs.FakeEventBridgeClient;
import nva.commons.core.Environment;
import org.junit.jupiter.api.Test;

class StartKeyBasedBatchHandlerTest {

    @Test
    void shouldSendEventWithTopic() throws JsonProcessingException {
        var client = new FakeEventBridgeClient();
        var handler = new StartKeyBasedBatchHandler(client);
        handler.handleRequest(null, null, mock(Context.class));

        var eventDetail = client.getRequestEntries().get(0).detail();
        var keyBatchRequest = JsonUtils.dtoObjectMapper.readValue(eventDetail, KeyBatchRequestEvent.class);

        assertThat(client.getRequestEntries(), hasSize(1));
        assertThat(keyBatchRequest, is(equalTo(new KeyBatchRequestEvent(null, new Environment().readEnv("TOPIC")))));
    }
}