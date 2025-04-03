package no.unit.nva.search;

import static no.unit.nva.search.resource.ResourceParameter.AGGREGATION;
import static no.unit.nva.search.resource.ResourceParameter.FROM;
import static no.unit.nva.search.resource.ResourceParameter.SIZE;
import static no.unit.nva.search.resource.ResourceParameter.SORT;

import com.amazonaws.services.lambda.runtime.Context;
import java.net.HttpURLConnection;
import java.net.http.HttpClient;
import no.unit.nva.search.resource.ResourceClient;
import no.unit.nva.search.resource.ResourceSearchQuery;
import no.unit.nva.search.resource.SimplifiedMutator;
import nva.commons.apigateway.AccessRight;
import nva.commons.apigateway.ApiGatewayHandler;
import nva.commons.apigateway.RequestInfo;
import nva.commons.apigateway.exceptions.ApiGatewayException;
import nva.commons.apigateway.exceptions.ForbiddenException;
import nva.commons.apigateway.exceptions.UnauthorizedException;
import nva.commons.core.Environment;
import nva.commons.core.JacocoGenerated;

public class UserBasedResourceSearchHandler extends ApiGatewayHandler<Void, String> {
  private final ResourceClient resourceClient;

  @JacocoGenerated
  public UserBasedResourceSearchHandler() {
    this(new Environment(), HttpClient.newHttpClient(), ResourceClient.defaultClient());
  }

  public UserBasedResourceSearchHandler(
      Environment environment, HttpClient httpClient, ResourceClient resourceClient) {
    super(Void.class, environment, httpClient);
    this.resourceClient = resourceClient;
  }

  @Override
  protected void validateRequest(Void input, RequestInfo requestInfo, Context context)
      throws ApiGatewayException {}

  @Override
  protected String processInput(Void input, RequestInfo requestInfo, Context context)
      throws ApiGatewayException {

    var username = authorize(requestInfo);

    return ResourceSearchQuery.builder()
        .fromRequestInfo(requestInfo)
        .withRequiredParameters(FROM, SIZE, AGGREGATION, SORT)
        .withAlwaysIncludedFields(SimplifiedMutator.getIncludedFields())
        .validate()
        .build()
        .withFilter()
        .ownResourcesOnly(username)
        .apply()
        .doSearch(resourceClient)
        .withMutators(new SimplifiedMutator())
        .toString();
  }

  private String authorize(RequestInfo requestInfo)
      throws ForbiddenException, UnauthorizedException {
    var username = requestInfo.getUserName();
    if (!requestInfo.getAccessRights().contains(AccessRight.MANAGE_OWN_RESOURCES)) {
      throw new ForbiddenException();
    }

    return username;
  }

  @Override
  protected Integer getSuccessStatusCode(Void input, String output) {
    return HttpURLConnection.HTTP_OK;
  }
}
