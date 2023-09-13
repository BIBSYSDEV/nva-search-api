package no.unit.nva.search2;

import com.amazonaws.services.lambda.runtime.Context;
import no.unit.nva.search2.common.OpenSearchSwsClient;
import no.unit.nva.search2.model.PagedSearchResourceDto;
import nva.commons.apigateway.ApiGatewayHandler;
import nva.commons.apigateway.RequestInfo;
import nva.commons.apigateway.exceptions.ApiGatewayException;
import nva.commons.core.Environment;
import nva.commons.core.JacocoGenerated;
import org.apache.http.HttpStatus;

import static no.unit.nva.search2.ResourceParameterKey.PAGE;
import static no.unit.nva.search2.ResourceParameterKey.PER_PAGE;
import static no.unit.nva.search2.ResourceParameterKey.SORT;
import static no.unit.nva.search2.ResourceParameterKey.SORT_ORDER;
import static no.unit.nva.search2.common.OpenSearchSwsClient.defaultSwsClient;

public class ResourcePagedSearchHandler extends ApiGatewayHandler<Void, PagedSearchResourceDto> {

    private final OpenSearchSwsClient openSearchSwsClient;

    @JacocoGenerated
    public ResourcePagedSearchHandler() {
        this(new Environment(), defaultSwsClient());
    }

    public ResourcePagedSearchHandler(Environment environment, OpenSearchSwsClient openSearchSwsClient) {
        super(Void.class, environment);
        this.openSearchSwsClient = openSearchSwsClient;
    }

    @Override
    protected PagedSearchResourceDto processInput(Void input, RequestInfo requestInfo, Context context)
        throws ApiGatewayException {
        return
            ResourceQuery.Builder.queryBuilder()
                .fromRequestInfo(requestInfo)
                .withRequiredParameters(PAGE, PER_PAGE,SORT,SORT_ORDER)
                .validate()
                .build()
                .doSearch(openSearchSwsClient);
    }

    @Override
    protected Integer getSuccessStatusCode(Void input, PagedSearchResourceDto output) {
        return HttpStatus.SC_OK;
    }
}
