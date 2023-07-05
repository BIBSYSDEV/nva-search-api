package no.unit.nva.search;

import static no.unit.nva.commons.json.JsonUtils.dtoObjectMapper;
import static no.unit.nva.search.RequestUtil.toQuery;
import static no.unit.nva.search.SearchClient.defaultSearchClient;
import static no.unit.nva.search.constants.ApplicationConstants.OPENSEARCH_ENDPOINT_INDEX;
import static no.unit.nva.search.constants.ApplicationConstants.RESOURCES_AGGREGATIONS;
import static no.unit.nva.search.constants.ApplicationConstants.objectMapperWithEmpty;
import static nva.commons.core.attempt.Try.attempt;
import com.amazonaws.services.lambda.runtime.Context;
import com.google.common.net.MediaType;
import java.util.List;
import no.unit.nva.search.models.SearchResponseDto;
import nva.commons.apigateway.ApiGatewayHandler;
import nva.commons.apigateway.RequestInfo;
import nva.commons.apigateway.RestRequestHandler;
import nva.commons.apigateway.exceptions.ApiGatewayException;
import nva.commons.apigateway.exceptions.UnsupportedAcceptHeaderException;
import nva.commons.core.Environment;
import nva.commons.core.JacocoGenerated;
import org.apache.http.HttpStatus;

public class SearchResourcesApiHandler extends ApiGatewayHandler<Void, String> {

    private final SearchClient openSearchClient;

    @JacocoGenerated
    public SearchResourcesApiHandler() {
        this(new Environment(), defaultSearchClient());
    }

    public SearchResourcesApiHandler(Environment environment, SearchClient openSearchClient) {
        super(Void.class, environment, objectMapperWithEmpty);
        this.openSearchClient = openSearchClient;
    }

    /**
     * Implements the main logic of the handler. Any exception thrown by this method will be handled by
     * {@link RestRequestHandler#handleExpectedException} method.
     *
     * @param input       The input object to the method. Usually a deserialized json.
     * @param requestInfo Request headers and path.
     * @param context     the ApiGateway context.
     * @return the Response body that is going to be serialized in json
     * @throws ApiGatewayException all exceptions are caught by writeFailure and mapped to error codes through the
     *                             method {@link RestRequestHandler#getFailureStatusCode}
     */
    @Override
    protected String processInput(Void input,
                                  RequestInfo requestInfo,
                                  Context context) throws ApiGatewayException {
        var query = toQuery(requestInfo, RESOURCES_AGGREGATIONS);
        var searchResponse = openSearchClient.searchWithSearchDocumentQuery(query,
                                                                          OPENSEARCH_ENDPOINT_INDEX);
        return createResponse(requestInfo, searchResponse);
    }

    private String createResponse(RequestInfo requestInfo, SearchResponseDto searchResponse)
        throws UnsupportedAcceptHeaderException {
        var contentType = getDefaultResponseContentTypeHeaderValue(requestInfo);

        if ("text".equalsIgnoreCase(contentType.type())) {
            return ExportSearchResources.exportSearchResults(searchResponse);
        } else {
            return attempt(() -> dtoObjectMapper.writeValueAsString(searchResponse)).orElseThrow();
        }
    }

    @Override
    protected List<MediaType> listSupportedMediaTypes() {
        return List.of(
            MediaType.JSON_UTF_8,
            MediaType.CSV_UTF_8
        );
    }

    /**
     * Define the success status code.
     *
     * @param input  The request input.
     * @param output The response output
     * @return the success status code.
     */
    @Override
    protected Integer getSuccessStatusCode(Void input, String output) {
        return HttpStatus.SC_OK;
    }
}
