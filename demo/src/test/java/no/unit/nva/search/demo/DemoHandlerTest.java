package no.unit.nva.search.demo;

import static java.net.HttpURLConnection.HTTP_BAD_REQUEST;
import static java.net.HttpURLConnection.HTTP_OK;
import static no.unit.nva.testutils.RandomDataGenerator.randomString;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsEqual.equalTo;
import com.amazonaws.services.lambda.runtime.Context;
import com.fasterxml.jackson.core.JsonProcessingException;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import no.unit.nva.commons.json.JsonUtils;
import no.unit.nva.stubs.FakeContext;
import no.unit.nva.testutils.HandlerRequestBuilder;
import nva.commons.apigateway.GatewayResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.zalando.problem.Problem;

class DemoHandlerTest {
    
    private Context context;
    private ByteArrayOutputStream outputStream;
    private DemoHandler handler;
    
    @BeforeEach
    public void setup() {
        this.context = new FakeContext();
        this.outputStream = new ByteArrayOutputStream();
        this.handler = new DemoHandler();
    }
    
    @Test
    void shouldEchoInput() throws IOException {
        String expectedName = randomString();
        var input = new InputClass(expectedName);
        var request = createRequest(input);
        handler.handleRequest(request, outputStream, context);
        var response = GatewayResponse.fromOutputStream(outputStream, OutputClass.class);
        var body = response.getBodyObject(OutputClass.class);
        assertThat(response.getStatusCode(), is(equalTo(HTTP_OK)));
        assertThat(body.getName(), is(equalTo(expectedName)));
    }
    
    @Test
    void shouldReturnBadRequestWhenInputHasNoName() throws IOException {
        var request = createRequest(InputClass.empty());
        handler.handleRequest(request, outputStream, context);
        var response = GatewayResponse.fromOutputStream(outputStream, Problem.class);
        var body = response.getBodyObject(Problem.class);
        assertThat(response.getStatusCode(),is(equalTo(HTTP_BAD_REQUEST)));
        assertThat(body.getDetail(),is(equalTo(DemoHandler.NO_DATA_ERROR)));
        
        
    }
    
    private InputStream createRequest(InputClass input) throws JsonProcessingException {
        return new HandlerRequestBuilder<InputClass>(JsonUtils.dtoObjectMapper)
            .withBody(input)
            .build();
    }
}