package no.unit.nva.search2;

import com.amazonaws.services.lambda.runtime.Context;
import no.unit.nva.search.models.SearchResponseDto;
import nva.commons.apigateway.ApiGatewayHandler;
import nva.commons.apigateway.RequestInfo;
import nva.commons.apigateway.exceptions.ApiGatewayException;
import nva.commons.core.Environment;
import nva.commons.core.JacocoGenerated;
import org.apache.http.HttpStatus;

import static no.unit.nva.search2.SwsQueryClient.defaultSwsClient;


public class SwsResourceHandler  extends ApiGatewayHandler<Void, SearchResponseDto> {

    private final SwsQueryClient swsQueryClient;

    @JacocoGenerated
    public SwsResourceHandler() {
        this(new Environment(), defaultSwsClient());
    }

    public SwsResourceHandler(Environment environment, SwsQueryClient swsQueryClient) {
        super(Void.class, environment);
        this.swsQueryClient = swsQueryClient;
    }

    @Override
    protected SearchResponseDto processInput(Void input, RequestInfo requestInfo, Context context)
        throws ApiGatewayException {

        return
            ResourceQuery.builder()
                .fromRequestInfo(requestInfo)
                .validate()
                .build()
                .execute(swsQueryClient);
    }

    @Override
    protected Integer getSuccessStatusCode(Void input, SearchResponseDto output) {
        return HttpStatus.SC_OK;
    }


}
