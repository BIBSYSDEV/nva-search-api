package no.sikt.nva.oai.pmh.handler.oaipmh.request;

import static java.util.Objects.nonNull;
import static no.sikt.nva.oai.pmh.handler.oaipmh.request.OaiPmhParameterName.RESUMPTION_TOKEN;

import no.sikt.nva.oai.pmh.handler.oaipmh.BadArgumentException;
import nva.commons.apigateway.RequestInfo;
import org.openarchives.oai.pmh.v2.VerbType;

public class ListSetsRequestFactory implements OaiPmhRequestFactory {

  @Override
  public OaiPmhRequest create(RequestInfo requestInfo, String body) {
    var resumptionToken =
        RequestUtils.extractParameter(RESUMPTION_TOKEN, requestInfo, body).orElse(null);
    if (nonNull(resumptionToken)) {
      throw new BadArgumentException(
          "Resumption token not supported for method '%s'".formatted(VerbType.LIST_SETS.value()));
    }
    return new ListSetsRequest();
  }
}
