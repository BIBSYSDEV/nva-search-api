package no.unit.nva.search;

import static no.unit.nva.constants.Defaults.DEFAULT_RESPONSE_MEDIA_TYPES;
import static no.unit.nva.search.resource.Constants.V_2024_12_01_SIMPLER_MODEL;
import static no.unit.nva.search.resource.ResourceClient.defaultClient;
import static no.unit.nva.search.resource.ResourceParameter.AGGREGATION;
import static no.unit.nva.search.resource.ResourceParameter.FROM;
import static no.unit.nva.search.resource.ResourceParameter.SIZE;
import static no.unit.nva.search.resource.ResourceParameter.SORT;

import com.amazonaws.services.lambda.runtime.Context;
import com.google.common.net.MediaType;
import java.net.HttpURLConnection;
import java.util.List;
import no.unit.nva.search.common.ContentTypeUtils;
import no.unit.nva.search.common.records.JsonNodeMutator;
import no.unit.nva.search.resource.LegacyMutator;
import no.unit.nva.search.resource.ResourceClient;
import no.unit.nva.search.resource.ResourceSearchQuery;
import no.unit.nva.search.resource.SimplifiedMutator;
import nva.commons.apigateway.AccessRight;
import nva.commons.apigateway.ApiGatewayHandler;
import nva.commons.apigateway.RequestInfo;
import nva.commons.apigateway.exceptions.ApiGatewayException;
import nva.commons.apigateway.exceptions.BadRequestException;
import nva.commons.apigateway.exceptions.UnauthorizedException;
import nva.commons.core.JacocoGenerated;

/**
 * Handler for searching resources.
 *
 * <p>Searches for resources in the OpenSearch index.
 */
public class SearchResourceAuthHandler extends ApiGatewayHandler<Void, String> {

  private final ResourceClient opensearchClient;

  @JacocoGenerated
  public SearchResourceAuthHandler() {
    this(defaultClient());
  }

  public SearchResourceAuthHandler(ResourceClient resourceClient) {
    super(Void.class);
    this.opensearchClient = resourceClient;
  }

  @Override
  protected List<MediaType> listSupportedMediaTypes() {
    return DEFAULT_RESPONSE_MEDIA_TYPES;
  }

  @Override
  protected void validateRequest(Void unused, RequestInfo requestInfo, Context context)
      throws ApiGatewayException {
    validateAccessRight(requestInfo.getAccessRights());
  }

  @Override
  protected String processInput(Void input, RequestInfo requestInfo, Context context)
      throws BadRequestException, UnauthorizedException {
    var version = ContentTypeUtils.extractVersionFromRequestInfo(requestInfo);

    return ResourceSearchQuery.builder()
        .fromRequestInfo(requestInfo)
        .withRequiredParameters(FROM, SIZE, AGGREGATION, SORT)
        .withAlwaysExcludedFields(getExcludedFields(version))
        .validate()
        .build()
        .withFilter()
        .customerCurationInstitutions(requestInfo)
        .apply()
        .doSearch(opensearchClient)
        .withMutators(getMutator(version))
        .toString();
  }

  @Override
  protected Integer getSuccessStatusCode(Void input, String output) {
    return HttpURLConnection.HTTP_OK;
  }

  /**
   * Validates that the user has the required access rights to search resources.
   * MANAGE_RESOURCES_ALL (editor) MANAGE_RESOURCES_STANDARD (any curator) MANAGE_CUSTOMERS (app
   * admin)
   *
   * @param accessRights the access rights of the user.
   * @throws UnauthorizedException if the user does not have the required access rights.
   */
  private void validateAccessRight(List<AccessRight> accessRights) throws UnauthorizedException {
    if (accessRights.contains(AccessRight.MANAGE_RESOURCES_ALL)
        || accessRights.contains(AccessRight.MANAGE_CUSTOMERS)
        || accessRights.contains(AccessRight.MANAGE_RESOURCES_STANDARD)) {
      return;
    }
    throw new UnauthorizedException();
  }

  private List<String> getExcludedFields(String version) {
    return V_2024_12_01_SIMPLER_MODEL.equals(version)
        ? SimplifiedMutator.getExcludedFields()
        : LegacyMutator.getExcludedFields();
  }

  private JsonNodeMutator getMutator(String version) {
    return V_2024_12_01_SIMPLER_MODEL.equals(version)
        ? new SimplifiedMutator()
        : new LegacyMutator();
  }
}
