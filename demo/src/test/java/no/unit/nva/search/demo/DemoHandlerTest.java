package no.unit.nva.search.demo;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsEqual.equalTo;
import com.amazonaws.services.lambda.runtime.Context;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import no.unit.nva.commons.json.JsonUtils;
import no.unit.nva.stubs.FakeContext;
import nva.commons.core.ioutils.IoUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class DemoHandlerTest {
    
    private Context context;
    private ByteArrayOutputStream outputStream;
    
    @BeforeEach
    public void setup() {
        this.context = new FakeContext();
        this.outputStream = new ByteArrayOutputStream();
    }
    
    @Test
    void shouldEchoInput() throws IOException {
        var input = new InputClass();
        input.setName("orestis");
        var inputStream = IoUtils.stringToStream(input.toString());
        var handler = new DemoHandler();
        handler.handleRequest(inputStream, outputStream, context);
        var outputString = outputStream.toString();
        var output = JsonUtils.dtoObjectMapper.readValue(outputString, InputClass.class);
        assertThat(output, is(equalTo(input)));
    }
}