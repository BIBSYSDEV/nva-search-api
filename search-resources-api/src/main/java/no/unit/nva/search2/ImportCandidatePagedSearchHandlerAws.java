package no.unit.nva.search2;

import static no.unit.nva.search2.ImportCandidateClient.defaultClient;
import static no.unit.nva.search2.constant.Defaults.DEFAULT_RESPONSE_MEDIA_TYPES;
import static no.unit.nva.search2.enums.ImportCandidateParameter.FROM;
import static no.unit.nva.search2.enums.ImportCandidateParameter.SIZE;
import static no.unit.nva.search2.enums.ImportCandidateParameter.SORT;
import com.amazonaws.services.lambda.runtime.Context;
import com.google.common.net.MediaType;
import java.net.HttpURLConnection;
import java.util.List;
import nva.commons.apigateway.ApiGatewayHandler;
import nva.commons.apigateway.RequestInfo;
import nva.commons.apigateway.exceptions.BadRequestException;
import nva.commons.core.Environment;
import nva.commons.core.JacocoGenerated;

public class ImportCandidatePagedSearchHandlerAws extends ApiGatewayHandler<Void, String> {

    private final ImportCandidateClient openSearchAwsClient;
    
    @JacocoGenerated
    public ImportCandidatePagedSearchHandlerAws() {
        this(new Environment(), defaultClient());
    }
    
    public ImportCandidatePagedSearchHandlerAws(Environment environment,
                                                ImportCandidateClient openSearchAwsClient) {
        super(Void.class, environment);
        this.openSearchAwsClient = openSearchAwsClient;
    }
    
    @Override
    protected String processInput(Void input, RequestInfo requestInfo, Context context) throws BadRequestException {
        return
            ImportCandidateQuery.builder()
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
