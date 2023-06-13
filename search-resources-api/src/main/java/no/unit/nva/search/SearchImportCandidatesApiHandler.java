package no.unit.nva.search;

import static no.unit.nva.search.RequestUtil.toQuery;
import static no.unit.nva.search.SearchClient.defaultSearchClient;
import static no.unit.nva.search.constants.ApplicationConstants.IMPORT_CANDIDATES_AGGREGATIONS;
import static no.unit.nva.search.constants.ApplicationConstants.IMPORT_CANDIDATES_INDEX;
import static no.unit.nva.search.constants.ApplicationConstants.objectMapperWithEmpty;
import com.amazonaws.services.lambda.runtime.Context;
import java.net.HttpURLConnection;
import no.unit.nva.search.models.SearchResponseDto;
import nva.commons.apigateway.ApiGatewayHandler;
import nva.commons.apigateway.RequestInfo;
import nva.commons.apigateway.exceptions.ApiGatewayException;
import nva.commons.core.Environment;
import nva.commons.core.JacocoGenerated;

public class SearchImportCandidatesApiHandler extends ApiGatewayHandler<Void, SearchResponseDto> {

    private final SearchClient openSearchClient;

    @JacocoGenerated
    public SearchImportCandidatesApiHandler() {
        this(new Environment(), defaultSearchClient());
    }

    public SearchImportCandidatesApiHandler(Environment environment, SearchClient openSearchClient) {
        super(Void.class, environment, objectMapperWithEmpty);
        this.openSearchClient = openSearchClient;
    }

    @Override
    protected SearchResponseDto processInput(Void input, RequestInfo requestInfo, Context context)
        throws ApiGatewayException {
        var query = toQuery(requestInfo, IMPORT_CANDIDATES_AGGREGATIONS);
        return openSearchClient.searchWithSearchDocumentQuery(query, IMPORT_CANDIDATES_INDEX);
    }

    @Override
    protected Integer getSuccessStatusCode(Void input, SearchResponseDto output) {
        return HttpURLConnection.HTTP_OK;
    }
}
