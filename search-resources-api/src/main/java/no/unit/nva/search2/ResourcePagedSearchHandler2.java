package no.unit.nva.search2;

import com.amazonaws.services.lambda.runtime.Context;
import no.unit.nva.search2.aws.OpenSearchAwsClient;
import no.unit.nva.search2.model.PagedSearchResourceDto;
import no.unit.nva.search2.sws.ResourceQuery;
import nva.commons.apigateway.ApiGatewayHandler;
import nva.commons.apigateway.RequestInfo;
import nva.commons.apigateway.exceptions.ApiGatewayException;
import nva.commons.core.Environment;
import nva.commons.core.JacocoGenerated;
import org.apache.http.HttpStatus;

import static no.unit.nva.search2.common.ResourceParameterKey.FROM;
import static no.unit.nva.search2.common.ResourceParameterKey.SIZE;
import static no.unit.nva.search2.common.ResourceParameterKey.SORT;
import static no.unit.nva.search2.aws.OpenSearchAwsClient.defaultClient;

public class ResourcePagedSearchHandler2 extends ApiGatewayHandler<Void, PagedSearchResourceDto> {

    private final OpenSearchAwsClient openSearchAwsClient;

    @JacocoGenerated
    public ResourcePagedSearchHandler2() {
        this(new Environment(), defaultClient());
    }

    public ResourcePagedSearchHandler2(Environment environment, OpenSearchAwsClient openSearchAwsClient) {
        super(Void.class, environment);
        this.openSearchAwsClient = openSearchAwsClient;
    }

    @Override
    protected PagedSearchResourceDto processInput(Void input, RequestInfo requestInfo, Context context)
        throws ApiGatewayException {
        return
            ResourceQuery.Builder.queryBuilder()
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
