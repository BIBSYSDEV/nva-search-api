package no.sikt.nva.oai.pmh.handler;

import com.amazonaws.services.lambda.runtime.Context;
import com.google.common.net.MediaType;
import jakarta.xml.bind.JAXBContext;
import jakarta.xml.bind.JAXBException;
import java.net.HttpURLConnection;
import java.util.List;
import nva.commons.apigateway.ApiGatewayHandler;
import nva.commons.apigateway.RequestInfo;
import nva.commons.apigateway.exceptions.ApiGatewayException;
import nva.commons.core.Environment;
import nva.commons.core.JacocoGenerated;
import org.openarchives.oai.pmh.v2.OAIPMHtype;

public class OaiPmhHandler extends ApiGatewayHandler<Void, String> {
  private final XmlSerializer xmlSerializer;
  private final OaiPmhDataProvider dataProvider = new DefaultOaiPmhDataProvider();

  @JacocoGenerated
  public OaiPmhHandler() throws JAXBException {
    super(Void.class, new Environment());

    var context = JAXBContext.newInstance(OAIPMHtype.class);
    var marshaller = context.createMarshaller();

    this.xmlSerializer = new JaxbXmlSerializer(marshaller);
  }

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
