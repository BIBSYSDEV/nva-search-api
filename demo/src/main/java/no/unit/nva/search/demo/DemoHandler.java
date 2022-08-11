package no.unit.nva.search.demo;

import com.amazonaws.services.lambda.runtime.Context;
import java.net.HttpURLConnection;
import java.util.Optional;
import nva.commons.apigateway.ApiGatewayHandler;
import nva.commons.apigateway.RequestInfo;
import nva.commons.apigateway.exceptions.BadRequestException;
import nva.commons.core.JacocoGenerated;

public class DemoHandler extends ApiGatewayHandler<InputClass, OutputClass> {
    
    public static final String NO_DATA_ERROR = "We would like your name";
    
    @JacocoGenerated
    public DemoHandler() {
        super(InputClass.class);
    }
    
    @Override
    protected OutputClass processInput(InputClass input, RequestInfo requestInfo, Context context)
        throws BadRequestException {
        var name = extractName(input);
        return new OutputClass(name);
    }
    
    @Override
    protected Integer getSuccessStatusCode(InputClass input, OutputClass output) {
        return HttpURLConnection.HTTP_OK;
    }
    
    private String extractName(InputClass input) throws BadRequestException {
        return Optional.ofNullable(input)
            .map(InputClass::getName)
            .orElseThrow(() -> new BadRequestException(NO_DATA_ERROR));
    }
}
