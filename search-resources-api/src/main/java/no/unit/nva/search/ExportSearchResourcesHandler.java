package no.unit.nva.search;

import static no.unit.nva.search.RequestUtil.toQuery;
import static no.unit.nva.search.SearchClient.defaultSearchClient;
import static no.unit.nva.search.constants.ApplicationConstants.OPENSEARCH_ENDPOINT_INDEX;
import static no.unit.nva.search.constants.ApplicationConstants.objectMapperWithEmpty;
import com.amazonaws.services.lambda.runtime.Context;
import java.io.IOException;
import nva.commons.apigateway.ApiGatewayHandler;
import nva.commons.apigateway.RequestInfo;
import nva.commons.apigateway.exceptions.ApiGatewayException;
import nva.commons.core.Environment;
import nva.commons.core.JacocoGenerated;
import org.apache.http.HttpStatus;

public class ExportSearchResourcesHandler extends ApiGatewayHandler<Void, String> {

    private final SearchClient openSearchClient;

    @JacocoGenerated
    public ExportSearchResourcesHandler() {
        this(new Environment(), defaultSearchClient());
    }

    public ExportSearchResourcesHandler(Environment environment, SearchClient openSearchClient) {
        super(Void.class, environment, objectMapperWithEmpty);
        this.openSearchClient = openSearchClient;
    }

    @Override
    protected String processInput(Void input,
                                  RequestInfo requestInfo,
                                  Context context) throws ApiGatewayException {
        var query = toQuery(requestInfo, null);
        try {
            return openSearchClient.exportSearchWithDocumentQuery(query, OPENSEARCH_ENDPOINT_INDEX);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    @Override
    protected Integer getSuccessStatusCode(Void input, String output) {
        return HttpStatus.SC_OK;
    }
}
