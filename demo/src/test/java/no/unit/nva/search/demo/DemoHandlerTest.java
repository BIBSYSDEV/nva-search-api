package no.unit.nva.search.demo;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsEqual.equalTo;
import com.amazonaws.services.lambda.runtime.Context;
import no.unit.nva.stubs.FakeContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class DemoHandlerTest {
    
    private Context context;
    
    @BeforeEach
    public void setup() {
        this.context = new FakeContext();
    }
    
    @Test
    void shouldEchoInput() {
        var input = new InputClass();
        input.setName("orestis");
        var handler = new DemoHandler();
        var output = handler.handleRequest(input, context);
        assertThat(input, is(equalTo(output)));
    }
}