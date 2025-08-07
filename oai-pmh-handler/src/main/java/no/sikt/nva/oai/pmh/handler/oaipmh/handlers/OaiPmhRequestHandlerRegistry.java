package no.sikt.nva.oai.pmh.handler.oaipmh.handlers;

import static java.util.Objects.isNull;

import java.net.URI;
import java.util.EnumMap;
import java.util.Map;
import java.util.function.Supplier;
import no.sikt.nva.oai.pmh.handler.oaipmh.BadVerbException;
import no.sikt.nva.oai.pmh.handler.oaipmh.RecordTransformer;
import no.sikt.nva.oai.pmh.handler.oaipmh.request.OaiPmhRequest;
import no.sikt.nva.oai.pmh.handler.repository.ResourceRepository;
import org.openarchives.oai.pmh.v2.VerbType;

public class OaiPmhRequestHandlerRegistry {
  private final Map<VerbType, Supplier<OaiPmhRequestHandler<?>>> handlerSuppliers =
      new EnumMap<>(VerbType.class);

  public OaiPmhRequestHandlerRegistry(
      ResourceRepository resourceRepository,
      RecordTransformer recordTransformer,
      int batchSize,
      URI endpointUri) {
    handlerSuppliers.put(VerbType.LIST_SETS, () -> new ListSetsRequestHandler(resourceRepository));
    handlerSuppliers.put(
        VerbType.IDENTIFY, () -> new IdentifyRequestHandler(endpointUri, resourceRepository));
    handlerSuppliers.put(VerbType.LIST_METADATA_FORMATS, ListMetadataFormatsRequestHandler::new);
    handlerSuppliers.put(
        VerbType.LIST_RECORDS,
        () -> new ListRecordsRequestHandler(resourceRepository, recordTransformer, batchSize));
    handlerSuppliers.put(
        VerbType.GET_RECORD,
        () -> new GetRecordRequestHandler(resourceRepository, recordTransformer));
  }

  @SuppressWarnings("unchecked")
  public <T extends OaiPmhRequest> OaiPmhRequestHandler<T> getHandler(VerbType verbType) {
    var supplier = handlerSuppliers.get(verbType);
    if (isNull(supplier)) {
      throw new BadVerbException("Unsupported verb: " + verbType);
    }

    return (OaiPmhRequestHandler<T>) supplier.get();
  }
}
