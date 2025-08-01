package no.sikt.nva.oai.pmh.handler.oaipmh.request;

import static no.sikt.nva.oai.pmh.handler.oaipmh.request.OaiPmhParameterName.VERB;

import java.util.Map;
import no.sikt.nva.oai.pmh.handler.oaipmh.BadVerbException;
import nva.commons.apigateway.RequestInfo;
import org.openarchives.oai.pmh.v2.VerbType;

public final class OaiPmhRequestFactoryRegistry {
  private static final String VERB_MISSING_MESSAGE = "Parameter 'verb' is missing.";
  private static final String NOT_SUPPORTED_VERB_MESSAGE =
      "Parameter 'verb' has a value that is not supported.";
  private static final Map<String, OaiPmhRequestFactory> REQUEST_FACTORIES_MAP =
      Map.of(
          VerbType.IDENTIFY.value(), new IdentifyRequestFactory(),
          VerbType.LIST_METADATA_FORMATS.value(), new ListMetadataFormatsRequestFactory(),
          VerbType.LIST_SETS.value(), new ListSetsRequestFactory(),
          VerbType.LIST_RECORDS.value(), new ListRecordsRequestFactory());

  private OaiPmhRequestFactoryRegistry() {}

  public static OaiPmhRequest from(RequestInfo requestInfo, String body) {
    final var verb =
        RequestUtils.extractParameter(VERB, requestInfo, body)
            .orElseThrow(() -> new BadVerbException(VERB_MISSING_MESSAGE));

    final var factory = REQUEST_FACTORIES_MAP.get(verb);
    if (factory == null) {
      throw new BadVerbException(NOT_SUPPORTED_VERB_MESSAGE);
    }
    return factory.create(requestInfo, body);
  }
}
