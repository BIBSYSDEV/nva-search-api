package no.sikt.nva.oai.pmh.handler;

import com.amazonaws.services.lambda.runtime.Context;
import com.google.common.net.MediaType;
import java.net.HttpURLConnection;
import java.util.List;
import nva.commons.apigateway.ApiGatewayHandler;
import nva.commons.apigateway.RequestInfo;
import nva.commons.apigateway.exceptions.ApiGatewayException;
import nva.commons.core.Environment;

public class OaiPmhHandler extends ApiGatewayHandler<Void, String> {
  private final XmlSerializer xmlSerializer;
  private final OaiPmhDataProvider dataProvider = new DefaultOaiPmhDataProvider();

  public OaiPmhHandler(Environment environment, XmlSerializer xmlSerializer) {
    super(Void.class, environment);
    this.xmlSerializer = xmlSerializer;
  }

  @Override
  protected void validateRequest(Void unused, RequestInfo requestInfo, Context context)
      throws ApiGatewayException {
    // no-op
  }

  @Override
  protected String processInput(Void unused, RequestInfo requestInfo, Context context)
      throws ApiGatewayException {

    return xmlSerializer.serialize(dataProvider.handleRequest());
  }

  @Override
  protected List<MediaType> listSupportedMediaTypes() {
    return List.of(MediaType.APPLICATION_XML_UTF_8);
  }

  @Override
  protected Integer getSuccessStatusCode(Void unused, String output) {
    return HttpURLConnection.HTTP_OK;
  }
}
