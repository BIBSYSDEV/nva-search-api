package no.unit.nva.search2;

import com.amazonaws.services.lambda.runtime.Context;
import no.unit.nva.search.models.SearchResponseDto;
import nva.commons.apigateway.ApiGatewayHandler;
import nva.commons.apigateway.RequestInfo;
import nva.commons.apigateway.exceptions.ApiGatewayException;
import nva.commons.core.Environment;
import nva.commons.core.JacocoGenerated;
import org.apache.http.HttpStatus;
import org.opensearch.action.search.SearchResponse;

import static no.unit.nva.search2.ResourceParameter.PAGE;
import static no.unit.nva.search2.ResourceParameter.PER_PAGE;
import static no.unit.nva.search2.SwsOpenSearchClient.defaultSwsClient;

public class SwsResourceHandler  extends ApiGatewayHandler<Void, SearchResponse> {

    private final SwsOpenSearchClient swsOpenSearchClient;

    @JacocoGenerated
    public SwsResourceHandler() {
        this(new Environment(), defaultSwsClient());
    }

    public SwsResourceHandler(Environment environment, SwsOpenSearchClient swsOpenSearchClient) {
        super(Void.class, environment);
        this.swsOpenSearchClient = swsOpenSearchClient;
    }

    @Override
    protected SearchResponse processInput(Void input, RequestInfo requestInfo, Context context)
        throws ApiGatewayException {

        return
            ResourceQuery.builder()
                .fromRequestInfo(requestInfo)
                .withRequiredParameters(PAGE, PER_PAGE)
                .validate()
                .build()
                .execute(swsOpenSearchClient);
    }

    @Override
    protected Integer getSuccessStatusCode(Void input, SearchResponse output) {
        return HttpStatus.SC_OK;
    }


}
