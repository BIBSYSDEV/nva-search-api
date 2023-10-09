package no.unit.nva.search2;

import static no.unit.nva.search2.ResourceAwsClient.defaultClient;
import static no.unit.nva.search2.model.ResourceParameterKey.FROM;
import static no.unit.nva.search2.model.ResourceParameterKey.SIZE;
import static no.unit.nva.search2.model.ResourceParameterKey.SORT;
import com.amazonaws.services.lambda.runtime.Context;
import nva.commons.apigateway.ApiGatewayHandler;
import nva.commons.apigateway.RequestInfo;
import nva.commons.apigateway.exceptions.ApiGatewayException;
import nva.commons.core.Environment;
import nva.commons.core.JacocoGenerated;
import org.apache.http.HttpStatus;

public class ResourcePagedSearchHandlerAws extends ApiGatewayHandler<Void, String> {

    private final ResourceAwsClient resourceAwsClient;

    @JacocoGenerated
    public ResourcePagedSearchHandlerAws() {
        this(new Environment(), defaultClient());
    }

    public ResourcePagedSearchHandlerAws(Environment environment, ResourceAwsClient resourceAwsClient) {
        super(Void.class, environment);
        this.resourceAwsClient = resourceAwsClient;
    }

    @Override
    protected String processInput(Void input, RequestInfo requestInfo, Context context)
        throws ApiGatewayException {
        return
            ResourceAwsQuery.builder()
                .fromRequestInfo(requestInfo)
                .withRequiredParameters(FROM, SIZE, SORT)
                .validate()
                .build()
                .doSearch(resourceAwsClient);
    }

    @Override
    protected Integer getSuccessStatusCode(Void input, String output) {
        return HttpStatus.SC_OK;
    }
}
