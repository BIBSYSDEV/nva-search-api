package no.unit.nva.search2;

import com.amazonaws.services.lambda.runtime.Context;
import no.unit.nva.search2.common.PagedSearchResponseDto;
import nva.commons.apigateway.ApiGatewayHandler;
import nva.commons.apigateway.RequestInfo;
import nva.commons.apigateway.exceptions.ApiGatewayException;
import nva.commons.core.Environment;
import nva.commons.core.JacocoGenerated;
import org.apache.http.HttpStatus;

import static no.unit.nva.search2.ResourceParameter.PAGE;
import static no.unit.nva.search2.ResourceParameter.PER_PAGE;
import static no.unit.nva.search2.ResourceParameter.SORT;
import static no.unit.nva.search2.ResourceParameter.SORT_ORDER;
import static no.unit.nva.search2.SwsOpenSearchClient.defaultSwsClient;

public class SwsPagedResourceHandler extends ApiGatewayHandler<Void, PagedSearchResponseDto> {

    private final SwsOpenSearchClient swsOpenSearchClient;

    @JacocoGenerated
    public SwsPagedResourceHandler() {
        this(new Environment(), defaultSwsClient());
    }

    public SwsPagedResourceHandler(Environment environment, SwsOpenSearchClient swsOpenSearchClient) {
        super(Void.class, environment);
        this.swsOpenSearchClient = swsOpenSearchClient;
    }

    @Override
    protected PagedSearchResponseDto processInput(Void input, RequestInfo requestInfo, Context context)
        throws ApiGatewayException {
        return
            ResourceQuery.builder()
                .fromRequestInfo(requestInfo)
                .withRequiredParameters(PAGE, PER_PAGE,SORT,SORT_ORDER)
                .validate()
                .build()
                .doSearch(swsOpenSearchClient);
    }

    @Override
    protected Integer getSuccessStatusCode(Void input, PagedSearchResponseDto output) {
        return HttpStatus.SC_OK;
    }
}
