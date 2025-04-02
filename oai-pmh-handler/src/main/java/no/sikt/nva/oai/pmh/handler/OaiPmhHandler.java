package no.sikt.nva.oai.pmh.handler;

import com.amazonaws.services.lambda.runtime.Context;
import com.google.common.net.MediaType;
import jakarta.xml.bind.JAXBContext;
import jakarta.xml.bind.JAXBException;
import java.net.HttpURLConnection;
import java.net.URI;
import java.util.List;
import no.unit.nva.search.resource.ResourceClient;
import nva.commons.apigateway.ApiGatewayHandler;
import nva.commons.apigateway.RequestInfo;
import nva.commons.apigateway.exceptions.ApiGatewayException;
import nva.commons.core.Environment;
import nva.commons.core.JacocoGenerated;
import nva.commons.core.StringUtils;
import nva.commons.core.paths.UriWrapper;
import org.openarchives.oai.pmh.v2.OAIPMHtype;

public class OaiPmhHandler extends ApiGatewayHandler<String, String> {

  private static final String QUERY_PARAM_VERB = "verb";
  private static final String EMPTY_VERB = "null";
  private static final String HTTPS_SCHEME = "https";
  private static final String NULL_STRING = "null";

  private final XmlSerializer xmlSerializer;
  private final OaiPmhDataProvider dataProvider;

  @JacocoGenerated
  public OaiPmhHandler() throws JAXBException {
    super(String.class);

    var endpointUri = generateEndpointUri(environment);

    var context = JAXBContext.newInstance(OAIPMHtype.class);
    var marshaller = context.createMarshaller();

    this.xmlSerializer = new JaxbXmlSerializer(marshaller);
    this.dataProvider = new DefaultOaiPmhDataProvider(ResourceClient.defaultClient(), endpointUri);
  }

  private static URI generateEndpointUri(Environment environment) {
    var apiHost = environment.readEnv("API_HOST");
    var basePath = environment.readEnv("OAI_BASE_PATH");
    return new UriWrapper(HTTPS_SCHEME, apiHost).addChild(basePath).getUri();
  }

  public OaiPmhHandler(
      Environment environment, XmlSerializer xmlSerializer, ResourceClient resourceClient) {
    super(String.class);

    var endpointUri = generateEndpointUri(environment);

    this.xmlSerializer = xmlSerializer;
    this.dataProvider = new DefaultOaiPmhDataProvider(resourceClient, endpointUri);
  }

  @Override
  protected void validateRequest(String body, RequestInfo requestInfo, Context context) {
    // no-op
  }

  @Override
  protected String processInput(String body, RequestInfo requestInfo, Context context)
      throws ApiGatewayException {
    var verb = extractVerb(requestInfo, body);

    return xmlSerializer.serialize(dataProvider.handleRequest(verb));
  }

  private String extractVerb(RequestInfo requestInfo, String body) {
    if (StringUtils.isEmpty(body) || NULL_STRING.equals(body)) {
      return requestInfo.getQueryParameterOpt(QUERY_PARAM_VERB).orElse(EMPTY_VERB);
    } else {
      var bodyParser = FormUrlencodedBodyParser.from(body);
      return bodyParser.getValue(QUERY_PARAM_VERB).orElse(EMPTY_VERB);
    }
  }

  @Override
  protected List<MediaType> listSupportedMediaTypes() {
    return List.of(MediaType.APPLICATION_XML_UTF_8);
  }

  @Override
  protected Integer getSuccessStatusCode(String input, String output) {
    return HttpURLConnection.HTTP_OK;
  }
}
