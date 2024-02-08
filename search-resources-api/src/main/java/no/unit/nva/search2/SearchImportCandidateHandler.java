package no.unit.nva.search2;

import com.amazonaws.services.lambda.runtime.Context;
import com.google.common.net.MediaType;
import java.net.HttpURLConnection;
import java.util.List;
import no.unit.nva.search2.importcandidate.ImportCandidateClient;
import no.unit.nva.search2.importcandidate.ImportCandidateQuery;
import nva.commons.apigateway.ApiGatewayHandler;
import nva.commons.apigateway.RequestInfo;
import nva.commons.apigateway.exceptions.BadRequestException;
import nva.commons.core.Environment;
import nva.commons.core.JacocoGenerated;

import static no.unit.nva.search2.constant.Defaults.DEFAULT_RESPONSE_MEDIA_TYPES;
import static no.unit.nva.search2.importcandidate.ImportCandidateClient.defaultClient;
import static no.unit.nva.search2.importcandidate.ImportCandidateParameter.FROM;
import static no.unit.nva.search2.importcandidate.ImportCandidateParameter.SIZE;

public class SearchImportCandidateHandler extends ApiGatewayHandler<Void, String> {

    private final ImportCandidateClient opensearchClient;
    
    @JacocoGenerated
    public SearchImportCandidateHandler() {
        this(new Environment(), defaultClient());
    }

    public SearchImportCandidateHandler(Environment environment, ImportCandidateClient candidateClient) {
        super(Void.class, environment);
        this.opensearchClient = candidateClient;
    }
    
    @Override
    protected String processInput(Void input, RequestInfo requestInfo, Context context) throws BadRequestException {
        return
            ImportCandidateQuery.builder()
                .fromRequestInfo(requestInfo)
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
