package no.sikt.nva.oai.pmh.handler.oaipmh;

import static no.sikt.nva.oai.pmh.handler.oaipmh.OaiPmhUtils.baseResponse;

import jakarta.xml.bind.JAXBElement;
import java.net.URI;
import no.unit.nva.search.resource.ResourceClient;
import org.openarchives.oai.pmh.v2.OAIPMHerrorcodeType;
import org.openarchives.oai.pmh.v2.OAIPMHtype;
import org.openarchives.oai.pmh.v2.ObjectFactory;

public class DefaultOaiPmhMethodRouter implements OaiPmhMethodRouter {

  private final OaiPmhRequestHandlerRegistry handlerRegistry;

  public DefaultOaiPmhMethodRouter(ResourceClient resourceClient, int batchSize, URI endpointUri) {
    this.handlerRegistry =
        new OaiPmhRequestHandlerRegistry(
            resourceClient, new SimplifiedRecordTransformer(), batchSize, endpointUri);
  }

  @Override
  public JAXBElement<OAIPMHtype> handleRequest(final OaiPmhRequest request, final URI endpointUri) {

    try {
      // Delegate processing to the appropriate handler
      var handler = handlerRegistry.getHandler(request.getVerbType());
      return handler.handleRequest(request);
    } catch (OaiPmhException exception) {
      return error(exception.getCodeType(), exception.getMessage());
    }
  }

  public static JAXBElement<OAIPMHtype> error(OAIPMHerrorcodeType codeType, String message) {
    var objectFactory = new ObjectFactory();
    var response = baseResponse(objectFactory);
    var value = response.getValue();
    var error = objectFactory.createOAIPMHerrorType();
    error.setCode(codeType);
    error.setValue(message);

    value.getError().add(error);

    return response;
  }
}
