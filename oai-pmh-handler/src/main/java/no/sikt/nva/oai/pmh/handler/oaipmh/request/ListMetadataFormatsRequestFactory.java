package no.sikt.nva.oai.pmh.handler.oaipmh.request;

import static no.sikt.nva.oai.pmh.handler.oaipmh.request.OaiPmhParameterName.IDENTIFIER;

import nva.commons.apigateway.RequestInfo;

public class ListMetadataFormatsRequestFactory implements OaiPmhRequestFactory {

  @Override
  public OaiPmhRequest create(RequestInfo requestInfo, String body) {
    var identifier = RequestUtils.extractParameter(IDENTIFIER, requestInfo, body).orElse(null);
    return new ListMetadataFormatsRequest(identifier);
  }
}
