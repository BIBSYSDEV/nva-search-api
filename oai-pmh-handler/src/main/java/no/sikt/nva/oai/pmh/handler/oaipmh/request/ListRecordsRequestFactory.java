package no.sikt.nva.oai.pmh.handler.oaipmh.request;

import static java.util.Objects.nonNull;
import static no.sikt.nva.oai.pmh.handler.oaipmh.request.OaiPmhParameterName.FROM;
import static no.sikt.nva.oai.pmh.handler.oaipmh.request.OaiPmhParameterName.METADATA_PREFIX;
import static no.sikt.nva.oai.pmh.handler.oaipmh.request.OaiPmhParameterName.RESUMPTION_TOKEN;
import static no.sikt.nva.oai.pmh.handler.oaipmh.request.OaiPmhParameterName.SET;
import static no.sikt.nva.oai.pmh.handler.oaipmh.request.OaiPmhParameterName.UNTIL;

import no.sikt.nva.oai.pmh.handler.oaipmh.BadArgumentException;
import no.sikt.nva.oai.pmh.handler.oaipmh.MetadataPrefix;
import no.sikt.nva.oai.pmh.handler.oaipmh.OaiPmhDateTime;
import no.sikt.nva.oai.pmh.handler.oaipmh.ResumptionToken;
import no.sikt.nva.oai.pmh.handler.oaipmh.SetSpec;
import nva.commons.apigateway.RequestInfo;

public class ListRecordsRequestFactory implements OaiPmhRequestFactory {

  @Override
  public OaiPmhRequest create(RequestInfo requestInfo, String body) {
    var resumptionToken =
        RequestUtils.extractParameter(RESUMPTION_TOKEN, requestInfo, body).orElse(null);
    if (nonNull(resumptionToken)) {
      return new ListRecordsRequest(ResumptionToken.from(resumptionToken).orElseThrow());
    }

    var metadataPrefix =
        RequestUtils.extractParameter(METADATA_PREFIX, requestInfo, body)
            .orElseThrow(() -> new BadArgumentException("metadataPrefix is required"));
    var from = RequestUtils.extractParameter(FROM, requestInfo, body).orElse(null);
    var until = RequestUtils.extractParameter(UNTIL, requestInfo, body).orElse(null);
    var set = RequestUtils.extractParameter(SET, requestInfo, body).orElse(null);

    return new ListRecordsRequest(
        OaiPmhDateTime.from(from),
        OaiPmhDateTime.from(until),
        SetSpec.from(set),
        MetadataPrefix.fromPrefix(metadataPrefix));
  }
}
