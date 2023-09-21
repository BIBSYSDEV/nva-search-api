package no.unit.nva.search2;

import static no.unit.nva.search2.OpenSearchSwsClient.defaultClient;
import static no.unit.nva.search2.model.ResourceParameterKey.FROM;
import static no.unit.nva.search2.model.ResourceParameterKey.SIZE;
import static no.unit.nva.search2.model.ResourceParameterKey.SORT;
import com.amazonaws.services.lambda.runtime.Context;
import no.unit.nva.search2.model.PagedSearchResourceDto;
import nva.commons.apigateway.ApiGatewayHandler;
import nva.commons.apigateway.RequestInfo;
import nva.commons.apigateway.exceptions.ApiGatewayException;
import nva.commons.core.Environment;
import nva.commons.core.JacocoGenerated;
import org.apache.http.HttpStatus;

public class ResourcePagedSearchHandlerSws extends ApiGatewayHandler<Void, PagedSearchResourceDto> {

    private final OpenSearchSwsClient openSearchSwsClient;

    @JacocoGenerated
    public ResourcePagedSearchHandlerSws() {
        this(new Environment(), defaultClient());
    }

    public ResourcePagedSearchHandlerSws(Environment environment, OpenSearchSwsClient openSearchSwsClient) {
        super(Void.class, environment);
        this.openSearchSwsClient = openSearchSwsClient;
    }

    @Override
    protected PagedSearchResourceDto processInput(Void input, RequestInfo requestInfo, Context context)
        throws ApiGatewayException {
        return
            ResourceSwsQuery.Builder.queryBuilder()
                .fromRequestInfo(requestInfo)
                .withRequiredParameters(FROM, SIZE, SORT)
                .validate()
                .build()
                .doSearch(openSearchSwsClient);
    }

    @Override
    protected Integer getSuccessStatusCode(Void input, PagedSearchResourceDto output) {
        return HttpStatus.SC_OK;
    }
}
