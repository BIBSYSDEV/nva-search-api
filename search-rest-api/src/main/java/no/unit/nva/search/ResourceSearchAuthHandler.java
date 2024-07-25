package no.unit.nva.search;

import static no.unit.nva.search.common.constant.Defaults.DEFAULT_RESPONSE_MEDIA_TYPES;
import static no.unit.nva.search.common.enums.PublicationStatus.DELETED;
import static no.unit.nva.search.common.enums.PublicationStatus.PUBLISHED;
import static no.unit.nva.search.common.enums.PublicationStatus.PUBLISHED_METADATA;
import static no.unit.nva.search.common.enums.PublicationStatus.UNPUBLISHED;
import static no.unit.nva.search.resource.ResourceClient.defaultClient;
import static no.unit.nva.search.resource.ResourceParameter.AGGREGATION;
import static no.unit.nva.search.resource.ResourceParameter.FROM;
import static no.unit.nva.search.resource.ResourceParameter.SIZE;
import com.amazonaws.services.lambda.runtime.Context;
import com.google.common.net.MediaType;
import java.net.HttpURLConnection;
import java.util.List;

import no.unit.nva.search.resource.ResourceClient;
import no.unit.nva.search.resource.ResourceSearchQuery;
import nva.commons.apigateway.AccessRight;
import nva.commons.apigateway.ApiGatewayHandler;
import nva.commons.apigateway.RequestInfo;
import nva.commons.apigateway.exceptions.ApiGatewayException;
import nva.commons.apigateway.exceptions.BadRequestException;
import nva.commons.apigateway.exceptions.UnauthorizedException;
import nva.commons.core.Environment;
import nva.commons.core.JacocoGenerated;

public class ResourceSearchAuthHandler extends ApiGatewayHandler<Void, String> {

    private final ResourceClient opensearchClient;

    @JacocoGenerated
    public ResourceSearchAuthHandler() {
        this(new Environment(), defaultClient());
    }

    public ResourceSearchAuthHandler(Environment environment, ResourceClient resourceClient) {
        super(Void.class, environment);
        this.opensearchClient = resourceClient;
    }

    @Override
    protected String processInput(Void input, RequestInfo requestInfo, Context context)
        throws BadRequestException, UnauthorizedException {

        return
            ResourceSearchQuery.builder()
                .fromRequestInfo(requestInfo)
                .withRequiredParameters(FROM, SIZE, AGGREGATION)
                .validate()
                .build()
                .withFilter()
                .requiredStatus(PUBLISHED, PUBLISHED_METADATA, DELETED, UNPUBLISHED)
                .organization(requestInfo.getCurrentCustomer())
                .apply()
                .doSearch(opensearchClient).toString();
    }

    @Override
    protected Integer getSuccessStatusCode(Void input, String output) {
        return HttpURLConnection.HTTP_OK;
    }

    @Override
    protected List<MediaType> listSupportedMediaTypes() {
        return DEFAULT_RESPONSE_MEDIA_TYPES;
    }

    @Override
    protected void validateRequest(Void unused, RequestInfo requestInfo, Context context) throws ApiGatewayException {
        validateAccessRight(requestInfo.getAccessRights());
    }

    private void validateAccessRight(List<AccessRight> accessRights) throws UnauthorizedException {
        if (accessRights.contains(AccessRight.MANAGE_OWN_AFFILIATION)
            || accessRights.contains(AccessRight.MANAGE_RESOURCES_STANDARD)) {
            return;
        }

        throw new UnauthorizedException();
    }
}
