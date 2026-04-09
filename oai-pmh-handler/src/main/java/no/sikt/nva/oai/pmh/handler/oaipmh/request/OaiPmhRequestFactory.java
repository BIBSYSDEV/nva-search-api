package no.sikt.nva.oai.pmh.handler.oaipmh.request;

import nva.commons.apigateway.RequestInfo;

@FunctionalInterface
public interface OaiPmhRequestFactory {
  OaiPmhRequest create(RequestInfo requestInfo, String body);
}
