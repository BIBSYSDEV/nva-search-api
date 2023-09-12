package no.unit.nva.search2;

import com.amazonaws.services.lambda.runtime.Context;
import no.unit.nva.search2.model.ResourcePagedSearchResponseDto;
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
import static no.unit.nva.search2.OpenSearchSwsClient.defaultSwsClient;

public class SwsPagedResourceHandler extends ApiGatewayHandler<Void, ResourcePagedSearchResponseDto> {

    private final OpenSearchSwsClient openSearchSwsClient;

    @JacocoGenerated
    public SwsPagedResourceHandler() {
        this(new Environment(), defaultSwsClient());
    }

    public SwsPagedResourceHandler(Environment environment, OpenSearchSwsClient openSearchSwsClient) {
        super(Void.class, environment);
        this.openSearchSwsClient = openSearchSwsClient;
    }

    @Override
    protected ResourcePagedSearchResponseDto processInput(Void input, RequestInfo requestInfo, Context context)
        throws ApiGatewayException {
        return
            OpenSearchResourceQuery.Builder.queryBuilder()
                .fromRequestInfo(requestInfo)
                .withRequiredParameters(PAGE, PER_PAGE,SORT,SORT_ORDER)
                .validate()
                .build()
                .doSearch(openSearchSwsClient);
    }

    @Override
    protected Integer getSuccessStatusCode(Void input, ResourcePagedSearchResponseDto output) {
        return HttpStatus.SC_OK;
    }
}
