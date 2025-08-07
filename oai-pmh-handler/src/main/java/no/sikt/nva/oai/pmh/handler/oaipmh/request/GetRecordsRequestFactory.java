package no.sikt.nva.oai.pmh.handler.oaipmh.request;

import static no.sikt.nva.oai.pmh.handler.oaipmh.request.OaiPmhParameterName.IDENTIFIER;
import static no.sikt.nva.oai.pmh.handler.oaipmh.request.OaiPmhParameterName.METADATA_PREFIX;

import no.sikt.nva.oai.pmh.handler.oaipmh.BadArgumentException;
import no.sikt.nva.oai.pmh.handler.oaipmh.MetadataPrefix;
import nva.commons.apigateway.RequestInfo;

public class GetRecordsRequestFactory implements OaiPmhRequestFactory {

  @Override
  public OaiPmhRequest create(RequestInfo requestInfo, String body) {
    var identifier = RequestUtils.extractParameter(IDENTIFIER, requestInfo, body).orElse(null);
    var metadataPrefix =
        RequestUtils.extractParameter(METADATA_PREFIX, requestInfo, body)
            .orElseThrow(() -> new BadArgumentException("metadataPrefix is required"));
    return new GetRecordRequest(identifier, MetadataPrefix.fromPrefix(metadataPrefix));
  }
}
