package no.unit.nva.search;

import static no.unit.nva.constants.Defaults.DEFAULT_RESPONSE_MEDIA_TYPES;
import static no.unit.nva.search.importcandidate.ImportCandidateClient.defaultClient;
import static no.unit.nva.search.importcandidate.ImportCandidateParameter.AGGREGATION;
import static no.unit.nva.search.importcandidate.ImportCandidateParameter.FROM;
import static no.unit.nva.search.importcandidate.ImportCandidateParameter.SIZE;

import com.amazonaws.services.lambda.runtime.Context;
import com.google.common.net.MediaType;
import java.net.HttpURLConnection;
import java.net.http.HttpClient;
import java.util.List;
import no.unit.nva.search.importcandidate.ImportCandidateClient;
import no.unit.nva.search.importcandidate.ImportCandidateSearchQuery;
import nva.commons.apigateway.ApiGatewayHandler;
import nva.commons.apigateway.RequestInfo;
import nva.commons.apigateway.exceptions.ApiGatewayException;
import nva.commons.apigateway.exceptions.BadRequestException;
import nva.commons.core.Environment;
import nva.commons.core.JacocoGenerated;

/**
 * Handler for searching import candidates.
 *
 * @author Stig Norland
 * @author Lars-Olav Vågene
 */
public class SearchImportCandidateAuthHandler extends ApiGatewayHandler<Void, String> {

  private final ImportCandidateClient opensearchClient;

  @JacocoGenerated
  public SearchImportCandidateAuthHandler() {
    this(new Environment(), defaultClient(), HttpClient.newHttpClient());
  }

  public SearchImportCandidateAuthHandler(
      Environment environment, ImportCandidateClient candidateClient, HttpClient httpClient) {
    super(Void.class, environment, httpClient);
    this.opensearchClient = candidateClient;
  }

  @Override
  protected String processInput(Void input, RequestInfo requestInfo, Context context)
      throws BadRequestException {
    return ImportCandidateSearchQuery.builder()
        .fromRequestInfo(requestInfo)
        .withRequiredParameters(FROM, SIZE, AGGREGATION)
        .validate()
        .build()
        .doSearch(opensearchClient)
        .toString();
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
  protected void validateRequest(Void unused, RequestInfo requestInfo, Context context)
      throws ApiGatewayException {
    requestInfo.getUserName();
  }
}
