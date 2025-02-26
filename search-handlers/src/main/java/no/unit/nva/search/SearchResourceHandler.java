package no.unit.nva.search;

import static no.unit.nva.constants.Defaults.DEFAULT_RESPONSE_MEDIA_TYPES;
import static no.unit.nva.search.common.enums.PublicationStatus.PUBLISHED;
import static no.unit.nva.search.common.enums.PublicationStatus.PUBLISHED_METADATA;
import static no.unit.nva.search.resource.Constants.V_2024_12_01_SIMPLER_MODEL;
import static no.unit.nva.search.resource.ResourceClient.defaultClient;
import static no.unit.nva.search.resource.ResourceParameter.AGGREGATION;
import static no.unit.nva.search.resource.ResourceParameter.FROM;
import static no.unit.nva.search.resource.ResourceParameter.SIZE;
import static no.unit.nva.search.resource.ResourceParameter.SORT;

import com.amazonaws.services.lambda.runtime.Context;
import com.google.common.net.MediaType;
import java.net.HttpURLConnection;
import java.net.http.HttpClient;
import java.util.List;
import java.util.Map;
import no.unit.nva.search.common.ContentTypeUtils;
import no.unit.nva.search.common.records.JsonNodeMutator;
import no.unit.nva.search.resource.LegacyMutator;
import no.unit.nva.search.resource.ResourceClient;
import no.unit.nva.search.resource.ResourceSearchQuery;
import no.unit.nva.search.resource.SimplifiedMutator;
import nva.commons.apigateway.ApiGatewayHandler;
import nva.commons.apigateway.RequestInfo;
import nva.commons.apigateway.exceptions.BadRequestException;
import nva.commons.core.Environment;
import nva.commons.core.JacocoGenerated;
import org.apache.http.HttpHeaders;

/**
 * Handler for searching resources.
 *
 * <p>Searches for resources in the OpenSearch index.
 */
public class SearchResourceHandler extends ApiGatewayHandler<Void, String> {

  private final ResourceClient opensearchClient;

  @JacocoGenerated
  public SearchResourceHandler() {
    this(new Environment(), defaultClient(), HttpClient.newHttpClient());
  }

  public SearchResourceHandler(
      Environment environment, ResourceClient resourceClient, HttpClient httpClient) {
    super(Void.class, environment, httpClient);
    this.opensearchClient = resourceClient;
  }

  @Override
  protected List<MediaType> listSupportedMediaTypes() {
    return DEFAULT_RESPONSE_MEDIA_TYPES;
  }

  @Override
  protected void validateRequest(Void unused, RequestInfo requestInfo, Context context) {
    // Do nothing
  }

  @Override
  protected String processInput(Void input, RequestInfo requestInfo, Context context)
      throws BadRequestException {
    var version = ContentTypeUtils.extractVersionFromRequestInfo(requestInfo);

    addAdditionalHeaders(() -> Map.of(HttpHeaders.VARY, HttpHeaders.ACCEPT));

    return ResourceSearchQuery.builder()
        .fromRequestInfo(requestInfo)
        .withRequiredParameters(FROM, SIZE, AGGREGATION, SORT)
        .withAlwaysIncludedFields(getIncludedFields(version))
        .validate()
        .build()
        .withFilter()
        .requiredStatus(PUBLISHED, PUBLISHED_METADATA)
        .apply()
        .doSearch(opensearchClient)
        .withMutators(getMutator(version))
        .toString();
  }

  private List<String> getIncludedFields(String version) {
    return V_2024_12_01_SIMPLER_MODEL.equals(version)
        ? SimplifiedMutator.getIncludedFields()
        : LegacyMutator.getIncludedFields();
  }

  private JsonNodeMutator getMutator(String version) {
    return V_2024_12_01_SIMPLER_MODEL.equals(version)
        ? new SimplifiedMutator()
        : new LegacyMutator();
  }

  @Override
  protected Integer getSuccessStatusCode(Void input, String output) {
    return HttpURLConnection.HTTP_OK;
  }
}
