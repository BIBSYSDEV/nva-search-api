package no.unit.nva.search2;

import static no.unit.nva.search2.ResourceAwsClient.defaultClient;
import static no.unit.nva.search2.constant.Defaults.DEFAULT_RESPONSE_MEDIA_TYPES;
import static no.unit.nva.search2.model.ParameterKeyResources.FROM;
import static no.unit.nva.search2.model.ParameterKeyResources.SIZE;
import static no.unit.nva.search2.model.ParameterKeyResources.SORT;
import com.amazonaws.services.lambda.runtime.Context;
import com.google.common.net.MediaType;
import java.net.HttpURLConnection;
import java.util.List;
import nva.commons.apigateway.ApiGatewayHandler;
import nva.commons.apigateway.RequestInfo;
import nva.commons.apigateway.exceptions.BadRequestException;
import nva.commons.core.Environment;
import nva.commons.core.JacocoGenerated;

public class ResourcePagedSearchHandlerAws extends ApiGatewayHandler<Void, String> {

    private final ResourceAwsClient openSearchAwsClient;

    @JacocoGenerated
    public ResourcePagedSearchHandlerAws() {
        this(new Environment(), defaultClient());
    }

    public ResourcePagedSearchHandlerAws(Environment environment, ResourceAwsClient openSearchAwsClient) {
        super(Void.class, environment);
        this.openSearchAwsClient = openSearchAwsClient;
    }

    @Override
    protected String processInput(Void input, RequestInfo requestInfo, Context context) throws BadRequestException {
        return
            ResourceAwsQuery.builder()
                .fromRequestInfo(requestInfo)
                .withRequiredParameters(FROM, SIZE, SORT)
                .validate()
                .build()
                .doSearch(openSearchAwsClient);
    }

    @Override
    protected Integer getSuccessStatusCode(Void input, String output) {
        return HttpURLConnection.HTTP_OK;
    }


    @Override
    protected List<MediaType> listSupportedMediaTypes() {
        return DEFAULT_RESPONSE_MEDIA_TYPES;
    }

}
