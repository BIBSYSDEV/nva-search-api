package no.sikt.nva.oai.pmh.handler;

import com.amazonaws.services.lambda.runtime.Context;
import com.google.common.net.MediaType;
import jakarta.xml.bind.JAXBContext;
import jakarta.xml.bind.JAXBElement;
import jakarta.xml.bind.JAXBException;
import java.net.HttpURLConnection;
import java.net.URI;
import java.util.List;
import no.sikt.nva.oai.pmh.handler.oaipmh.DefaultOaiPmhMethodRouter;
import no.sikt.nva.oai.pmh.handler.oaipmh.OaiPmhException;
import no.sikt.nva.oai.pmh.handler.oaipmh.OaiPmhMethodRouter;
import no.sikt.nva.oai.pmh.handler.oaipmh.request.OaiPmhRequestFactoryRegistry;
import no.sikt.nva.oai.pmh.handler.repository.ResourceClientResourceRepository;
import no.unit.nva.search.resource.ResourceClient;
import nva.commons.apigateway.ApiGatewayHandler;
import nva.commons.apigateway.RequestInfo;
import nva.commons.core.Environment;
import nva.commons.core.JacocoGenerated;
import nva.commons.core.paths.UriWrapper;
import org.openarchives.oai.pmh.v2.OAIPMHtype;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class OaiPmhHandler extends ApiGatewayHandler<String, String> {
  private static final Logger logger = LoggerFactory.getLogger(OaiPmhHandler.class);

  private static final String HTTPS_SCHEME = "https";

  private final XmlSerializer xmlSerializer;
  private final OaiPmhMethodRouter dataProvider;
  private final URI endpointUri;

  @JacocoGenerated
  public OaiPmhHandler() throws JAXBException {
    this(new Environment(), defaultOaiPmhMethodRouter(), defaultXmlSerializer());
  }

  public OaiPmhHandler(
      Environment environment, OaiPmhMethodRouter dataProvider, XmlSerializer xmlSerializer) {
    super(String.class, environment);
    this.endpointUri = generateEndpointUri(environment);
    this.dataProvider = dataProvider;
    this.xmlSerializer = xmlSerializer;
  }

  @JacocoGenerated
  private static XmlSerializer defaultXmlSerializer() throws JAXBException {
    var context = JAXBContext.newInstance(OAIPMHtype.class);
    logger.info(
        "Created JAXBContext for OAIPMHtype with the following implementation: {}",
        context.getClass().getName());
    var marshaller = context.createMarshaller();
    JaxbUtils.configureMarshaller(marshaller);
    return new JaxbXmlSerializer(marshaller);
  }

  @JacocoGenerated
  private static OaiPmhMethodRouter defaultOaiPmhMethodRouter() {
    var environment = new Environment();
    var batchSize = environment.readEnvOpt("LIST_RECORDS_BATCH_SIZE").orElse("250");
    return new DefaultOaiPmhMethodRouter(
        new ResourceClientResourceRepository(ResourceClient.defaultClient()),
        Integer.parseInt(batchSize),
        generateEndpointUri(environment),
        generateIdentifierBaseUri(environment));
  }

  protected static URI generateEndpointUri(Environment environment) {
    var apiHost = environment.readEnv("API_HOST");
    var basePath = environment.readEnv("OAI_BASE_PATH");
    return new UriWrapper(HTTPS_SCHEME, apiHost).addChild(basePath).getUri();
  }

  protected static URI generateIdentifierBaseUri(Environment environment) {
    var apiHost = environment.readEnv("API_HOST");
    return new UriWrapper(HTTPS_SCHEME, apiHost).addChild("publication").getUri();
  }

  @Override
  protected void validateRequest(String body, RequestInfo requestInfo, Context context) {
    // no-op
  }

  @Override
  protected String processInput(String body, RequestInfo requestInfo, Context context) {
    JAXBElement<OAIPMHtype> response;
    try {
      var oaiPmhRequest = OaiPmhRequestFactoryRegistry.from(requestInfo, body);
      response = dataProvider.handleRequest(oaiPmhRequest, endpointUri);
    } catch (OaiPmhException exception) {
      response = DefaultOaiPmhMethodRouter.error(exception.getCodeType(), exception.getMessage());
    }
    return xmlSerializer.serialize(response);
  }

  @Override
  protected List<MediaType> listSupportedMediaTypes() {
    return List.of(MediaType.XML_UTF_8);
  }

  @Override
  protected Integer getSuccessStatusCode(String input, String output) {
    return HttpURLConnection.HTTP_OK;
  }
}
