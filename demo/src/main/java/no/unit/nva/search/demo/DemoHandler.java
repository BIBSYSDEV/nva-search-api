package no.unit.nva.search.demo;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;

public class DemoHandler implements RequestHandler<InputClass, InputClass> {
    
    @Override
    public InputClass handleRequest(InputClass input, Context context) {
        return input;
    }
}
