package no.unit.nva.search2;

import static no.unit.nva.search2.ResourceClient.defaultClient;
import static no.unit.nva.search2.constant.Defaults.DEFAULT_RESPONSE_MEDIA_TYPES;
import static no.unit.nva.search2.enums.PublicationStatus.DELETED;
import static no.unit.nva.search2.enums.PublicationStatus.PUBLISHED_METADATA;
import static no.unit.nva.search2.enums.PublicationStatus.PUBLSHED;
import static no.unit.nva.search2.enums.PublicationStatus.UNPUBLISHED;
import static no.unit.nva.search2.enums.ResourceParameter.FROM;
import static no.unit.nva.search2.enums.ResourceParameter.SIZE;
import com.amazonaws.services.lambda.runtime.Context;
import com.google.common.net.MediaType;
import java.net.HttpURLConnection;
import java.util.List;
import nva.commons.apigateway.ApiGatewayHandler;
import nva.commons.apigateway.RequestInfo;
import nva.commons.apigateway.exceptions.BadRequestException;
import nva.commons.core.Environment;
import nva.commons.core.JacocoGenerated;

public class SearchResourceAuthHandler extends ApiGatewayHandler<Void, String> {

    private final ResourceClient opensearchClient;

    @JacocoGenerated
    public SearchResourceAuthHandler() {
        this(new Environment(), defaultClient());
    }

    public SearchResourceAuthHandler(Environment environment, ResourceClient resourceClient) {
        super(Void.class, environment);
        this.opensearchClient = resourceClient;
    }

    @Override
    protected String processInput(Void input, RequestInfo requestInfo, Context context) throws BadRequestException {
        //TODO verify user access rights?
        return
            ResourceQuery.builder()
                .fromRequestInfo(requestInfo)
                .withStatus(PUBLSHED, PUBLISHED_METADATA, DELETED, UNPUBLISHED)
                .withRequiredParameters(FROM, SIZE)
                .validate()
                .build()
                .doSearch(opensearchClient);
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
