package no.unit.nva.search;

import com.amazonaws.services.lambda.runtime.Context;
import com.google.common.net.MediaType;
import no.unit.nva.search.importcandidate.ImportCandidateClient;
import no.unit.nva.search.importcandidate.ImportCandidateSearchQuery;
import nva.commons.apigateway.ApiGatewayHandler;
import nva.commons.apigateway.RequestInfo;
import nva.commons.apigateway.exceptions.BadRequestException;
import nva.commons.apigateway.exceptions.UnauthorizedException;
import nva.commons.core.Environment;
import nva.commons.core.JacocoGenerated;

import java.net.HttpURLConnection;
import java.util.List;
import java.util.Objects;

import static no.unit.nva.search.common.constant.Defaults.DEFAULT_RESPONSE_MEDIA_TYPES;
import static no.unit.nva.search.importcandidate.ImportCandidateClient.defaultClient;
import static no.unit.nva.search.importcandidate.ImportCandidateParameter.AGGREGATION;
import static no.unit.nva.search.importcandidate.ImportCandidateParameter.FROM;
import static no.unit.nva.search.importcandidate.ImportCandidateParameter.SIZE;

public class SearchImportCandidateAuthHandler extends ApiGatewayHandler<Void, String> {

    private final ImportCandidateClient opensearchClient;

    @JacocoGenerated
    public SearchImportCandidateAuthHandler() {
        this(new Environment(), defaultClient());
    }

    public SearchImportCandidateAuthHandler(Environment environment, ImportCandidateClient candidateClient) {
        super(Void.class, environment);
        this.opensearchClient = candidateClient;
    }

    @Override
    protected String processInput(Void input, RequestInfo requestInfo, Context context) throws BadRequestException, UnauthorizedException {

        validateAccessRight(requestInfo);

        return
            ImportCandidateSearchQuery.builder()
                .fromRequestInfo(requestInfo)
                .withRequiredParameters(FROM, SIZE, AGGREGATION)
                .validate()
                .build()
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

    private void validateAccessRight(RequestInfo requestInfo) throws UnauthorizedException {
        if (Objects.isNull(requestInfo.getUserName())) {
            throw new UnauthorizedException();
        }
    }
}
