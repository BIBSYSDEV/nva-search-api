package no.sikt.nva.oai.pmh.handler.oaipmh.request;

import nva.commons.apigateway.RequestInfo;

public class IdentifyRequestFactory implements OaiPmhRequestFactory {

  @Override
  public OaiPmhRequest create(RequestInfo requestInfo, String body) {
    return new IdentifyRequest();
  }
}
