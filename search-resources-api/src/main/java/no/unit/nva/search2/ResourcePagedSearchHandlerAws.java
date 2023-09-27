package no.unit.nva.search2;

import com.amazonaws.services.lambda.runtime.Context;
import no.unit.nva.search2.model.PagedSearchResourceDto;
import nva.commons.apigateway.ApiGatewayHandler;
import nva.commons.apigateway.RequestInfo;
import nva.commons.apigateway.exceptions.ApiGatewayException;
import nva.commons.core.Environment;
import nva.commons.core.JacocoGenerated;
import org.apache.http.HttpStatus;

import static no.unit.nva.search2.model.ResourceParameterKey.FROM;
import static no.unit.nva.search2.model.ResourceParameterKey.SIZE;
import static no.unit.nva.search2.model.ResourceParameterKey.SORT;
import static no.unit.nva.search2.OpenSearchAwsClient.defaultClient;

public class ResourcePagedSearchHandlerAws extends ApiGatewayHandler<Void, PagedSearchResourceDto> {

    private final OpenSearchAwsClient openSearchAwsClient;

    @JacocoGenerated
    public ResourcePagedSearchHandlerAws() {
        this(new Environment(), defaultClient());
    }

    public ResourcePagedSearchHandlerAws(Environment environment, OpenSearchAwsClient openSearchAwsClient) {
        super(Void.class, environment);
        this.openSearchAwsClient = openSearchAwsClient;
    }

    @Override
    protected PagedSearchResourceDto processInput(Void input, RequestInfo requestInfo, Context context)
        throws ApiGatewayException {
        return
            ResourceAwsQuery.Builder.queryBuilder()
                .fromRequestInfo(requestInfo)
                .withRequiredParameters(FROM, SIZE, SORT)
                .validate()
                .build()
                .doSearch(openSearchAwsClient);
    }

    @Override
    protected Integer getSuccessStatusCode(Void input, PagedSearchResourceDto output) {
        return HttpStatus.SC_OK;
    }
}
