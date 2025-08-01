package no.sikt.nva.oai.pmh.handler.oaipmh;

import static java.util.Objects.isNull;

import java.net.URI;
import java.util.EnumMap;
import java.util.Map;
import java.util.function.Supplier;
import no.unit.nva.search.resource.ResourceClient;
import org.openarchives.oai.pmh.v2.VerbType;

public class OaiPmhRequestHandlerRegistry {
  private final Map<VerbType, Supplier<OaiPmhRequestHandler<?>>> handlerSuppliers =
      new EnumMap<>(VerbType.class);

  public OaiPmhRequestHandlerRegistry(
      ResourceClient resourceClient,
      RecordTransformer recordTransformer,
      int batchSize,
      URI endpointUri) {
    handlerSuppliers.put(VerbType.LIST_SETS, () -> new ListSetsRequestHandler(resourceClient));
    handlerSuppliers.put(VerbType.IDENTIFY, () -> new IdentifyRequestHandler(endpointUri));
    handlerSuppliers.put(VerbType.LIST_METADATA_FORMATS, ListMetadataFormatsRequestHandler::new);
    handlerSuppliers.put(
        VerbType.LIST_RECORDS,
        () -> new ListRecordsRequestHandler(resourceClient, recordTransformer, batchSize));
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
