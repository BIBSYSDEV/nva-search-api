package no.sikt.nva.oai.pmh.handler.oaipmh;

import static no.sikt.nva.oai.pmh.handler.oaipmh.OaiPmhUtils.baseResponse;

import jakarta.xml.bind.JAXBElement;
import java.net.URI;
import no.sikt.nva.oai.pmh.handler.oaipmh.handlers.OaiPmhRequestHandlerRegistry;
import no.sikt.nva.oai.pmh.handler.oaipmh.request.OaiPmhRequest;
import no.sikt.nva.oai.pmh.handler.oaipmh.transformers.SimplifiedRecordTransformer;
import no.sikt.nva.oai.pmh.handler.repository.ResourceRepository;
import org.openarchives.oai.pmh.v2.OAIPMHerrorcodeType;
import org.openarchives.oai.pmh.v2.OAIPMHtype;
import org.openarchives.oai.pmh.v2.ObjectFactory;

public class DefaultOaiPmhMethodRouter implements OaiPmhMethodRouter {

  private final OaiPmhRequestHandlerRegistry handlerRegistry;

  public DefaultOaiPmhMethodRouter(
      ResourceRepository resourceRepository,
      int batchSize,
      URI endpointUri,
      URI identifierBaseUri) {
    this.handlerRegistry =
        new OaiPmhRequestHandlerRegistry(
            resourceRepository,
            new SimplifiedRecordTransformer(),
            batchSize,
            endpointUri,
            identifierBaseUri);
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
