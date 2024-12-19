package no.unit.nva.search.handlers;

import static no.unit.nva.search.model.constant.Defaults.DEFAULT_RESPONSE_MEDIA_TYPES;
import static no.unit.nva.search.model.enums.PublicationStatus.PUBLISHED;
import static no.unit.nva.search.model.enums.PublicationStatus.PUBLISHED_METADATA;
import static no.unit.nva.search.service.resource.ResourceClient.defaultClient;
import static no.unit.nva.search.service.resource.ResourceParameter.AGGREGATION;
import static no.unit.nva.search.service.resource.ResourceParameter.FROM;
import static no.unit.nva.search.service.resource.ResourceParameter.SIZE;
import static no.unit.nva.search.service.resource.ResourceParameter.SORT;

import com.amazonaws.services.lambda.runtime.Context;
import com.google.common.net.MediaType;

import no.unit.nva.search.service.resource.ResourceClient;
import no.unit.nva.search.service.resource.ResourceSearchQuery;
import no.unit.nva.search.service.resource.SimplifiedResourceModelMutator;

import nva.commons.apigateway.ApiGatewayHandler;
import nva.commons.apigateway.RequestInfo;
import nva.commons.apigateway.exceptions.BadRequestException;
import nva.commons.core.Environment;
import nva.commons.core.JacocoGenerated;

import java.net.HttpURLConnection;
import java.util.List;

/**
 * Handler for searching resources.
 *
 * <p>Searches for resources in the OpenSearch index.
 */
public class SearchResource20241201Handler extends ApiGatewayHandler<Void, String> {

    private final ResourceClient opensearchClient;

    @JacocoGenerated
    public SearchResource20241201Handler() {
        this(new Environment(), defaultClient());
    }

    public SearchResource20241201Handler(Environment environment, ResourceClient resourceClient) {
        super(Void.class, environment);
        this.opensearchClient = resourceClient;
    }

    @Override
    protected List<MediaType> listSupportedMediaTypes() {
        return DEFAULT_RESPONSE_MEDIA_TYPES;
    }

    @Override
    protected void validateRequest(Void unused, RequestInfo requestInfo, Context context) {
        // Do nothing
    }

    @Override
    protected String processInput(Void input, RequestInfo requestInfo, Context context)
            throws BadRequestException {
        return ResourceSearchQuery.builder()
                .fromRequestInfo(requestInfo)
                .withRequiredParameters(FROM, SIZE, AGGREGATION, SORT)
                .withAlwaysIncludedFields(SimplifiedResourceModelMutator.getIncludedFields())
                .validate()
                .build()
                .withFilter()
                .requiredStatus(PUBLISHED, PUBLISHED_METADATA)
                .apply()
                .doSearch(opensearchClient)
                .withMutators(new SimplifiedResourceModelMutator())
                .toString();
    }

    @Override
    protected Integer getSuccessStatusCode(Void input, String output) {
        return HttpURLConnection.HTTP_OK;
    }
}
