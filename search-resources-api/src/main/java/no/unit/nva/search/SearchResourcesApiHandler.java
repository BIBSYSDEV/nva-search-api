package no.unit.nva.search;

import static no.unit.nva.search.RequestUtil.toQuery;
import static no.unit.nva.search.SearchClient.defaultSearchClient;
import static no.unit.nva.search.constants.ApplicationConstants.AGGREGATIONS;
import static no.unit.nva.search.constants.ApplicationConstants.OPENSEARCH_ENDPOINT_INDEX;
import static no.unit.nva.search.constants.ApplicationConstants.objectMapperWithEmpty;
import com.amazonaws.services.lambda.runtime.Context;
import java.util.Arrays;
import no.unit.nva.search.exception.SearchException;
import no.unit.nva.search.models.SearchResponseDto;
import nva.commons.apigateway.ApiGatewayHandler;
import nva.commons.apigateway.RequestInfo;
import nva.commons.apigateway.RestRequestHandler;
import nva.commons.apigateway.exceptions.ApiGatewayException;
import nva.commons.core.Environment;
import nva.commons.core.JacocoGenerated;
import org.apache.http.HttpStatus;
import org.opensearch.OpenSearchStatusException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SearchResourcesApiHandler extends ApiGatewayHandler<Void, SearchResponseDto> {

    private static final Logger logger = LoggerFactory.getLogger(SearchResourcesApiHandler.class);
    private final SearchClient openSearchClient;
    private static final String TOO_MANY_NESTED_CLAUSES = "too_many_nested_clauses";
    public static final String TOO_MANY_NESTED_CLAUSES_FULL = "too_many_nested_clauses: Query contains too many nested "
                                                       + "clauses";

    @JacocoGenerated
    public SearchResourcesApiHandler() {
        this(new Environment(), defaultSearchClient());
    }

    public SearchResourcesApiHandler(Environment environment, SearchClient openSearchClient) {
        super(Void.class, environment, objectMapperWithEmpty);
        this.openSearchClient = openSearchClient;
    }

    /**
     * Implements the main logic of the handler. Any exception thrown by this method will be handled by {@link
     * RestRequestHandler#handleExpectedException} method.
     *
     * @param input       The input object to the method. Usually a deserialized json.
     * @param requestInfo Request headers and path.
     * @param context     the ApiGateway context.
     * @return the Response body that is going to be serialized in json
     * @throws ApiGatewayException all exceptions are caught by writeFailure and mapped to error codes through the
     *                             method {@link RestRequestHandler#getFailureStatusCode}
     */
    @Override
    protected SearchResponseDto processInput(Void input,
                                             RequestInfo requestInfo,
                                             Context context) throws ApiGatewayException {
        var query = toQuery(requestInfo, AGGREGATIONS);

        try {
            return openSearchClient.searchWithSearchDocumentQuery(query, OPENSEARCH_ENDPOINT_INDEX);
        } catch (OpenSearchStatusException e) {
            throw handleOpenSearchFailure(e);
        }
    }

    /**
     * Define the success status code.
     *
     * @param input  The request input.
     * @param output The response output
     * @return the success status code.
     */
    @Override
    protected Integer getSuccessStatusCode(Void input, SearchResponseDto output) {
        return HttpStatus.SC_OK;
    }

    private boolean exceptionIsTooManyClauses(OpenSearchStatusException exception) {
        var rootCause = exception.guessRootCauses();
        return Arrays.stream(rootCause)
                   .anyMatch(rc -> rc.getDetailedMessage().contains(TOO_MANY_NESTED_CLAUSES)
                        || Arrays.stream(rc.guessRootCauses())
                               .anyMatch( nrc -> nrc.getDetailedMessage().contains(TOO_MANY_NESTED_CLAUSES))) ;
    }

    @JacocoGenerated
    private ApiGatewayException handleOpenSearchFailure(OpenSearchStatusException exception) {
        logger.warn("Unhandled OpenSearchStatusException", exception.getMessage());

        exception.printStackTrace();
        logger.warn(exception.toString());
        var rooCause = exception.guessRootCauses();
        Arrays.stream(rooCause)
            .forEach(rc -> {
                logger.warn("toString: " + rc.toString());
                logger.warn("getMessage: " + rc.getMessage());
                logger.warn("getDetailedMessage: " + rc.getDetailedMessage());
            });

        if (exceptionIsTooManyClauses(exception)) {
            return new SearchException(TOO_MANY_NESTED_CLAUSES_FULL, exception);
        }

        throw new RuntimeException(exception);
    }
}
