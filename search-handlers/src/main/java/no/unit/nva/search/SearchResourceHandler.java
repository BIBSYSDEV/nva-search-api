package no.unit.nva.search;

import static no.unit.nva.constants.Defaults.DEFAULT_RESPONSE_MEDIA_TYPES;
import static no.unit.nva.search.resource.ResourceClient.defaultClient;

import com.amazonaws.services.lambda.runtime.Context;
import com.google.common.net.MediaType;

import no.unit.nva.search.common.ContentTypeUtils;
import no.unit.nva.search.resource.ResourceClient;

import nva.commons.apigateway.ApiGatewayHandler;
import nva.commons.apigateway.RequestInfo;
import nva.commons.apigateway.exceptions.BadRequestException;
import nva.commons.core.Environment;
import nva.commons.core.JacocoGenerated;

import org.apache.http.HttpHeaders;

import java.net.HttpURLConnection;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

/**
 * Handler for searching resources.
 *
 * <p>Searches for resources in the OpenSearch index.
 */
public class SearchResourceHandler extends ApiGatewayHandler<Void, String> {

    private final ResourceClient opensearchClient;

    private static final String V_2024_12_01_SIMPLER_MODEL = "2024-12-01";

    @JacocoGenerated
    public SearchResourceHandler() {
        this(new Environment(), defaultClient());
    }

    public SearchResourceHandler(Environment environment, ResourceClient resourceClient) {
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

        switch (ContentTypeUtils.extractVersionFromRequestInfo(requestInfo)) {
            case V_2024_12_01_SIMPLER_MODEL:
                return new SearchResource20241201Handler(environment, opensearchClient)
                        .processInput(input, requestInfo, context);
            case null:
            default:
                return new SearchResourceLegacyHandler(environment, opensearchClient)
                        .processInput(input, requestInfo, context);
        }
    }

    @Override
    protected void addAdditionalHeaders(Supplier<Map<String, String>> additionalHeaders) {
        super.addAdditionalHeaders(() -> Map.of(HttpHeaders.VARY, HttpHeaders.ACCEPT));
    }

    @Override
    protected Integer getSuccessStatusCode(Void input, String output) {
        return HttpURLConnection.HTTP_OK;
    }
}
