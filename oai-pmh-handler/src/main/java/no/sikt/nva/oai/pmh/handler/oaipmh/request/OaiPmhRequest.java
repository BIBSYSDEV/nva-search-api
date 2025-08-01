package no.sikt.nva.oai.pmh.handler.oaipmh.request;

import org.openarchives.oai.pmh.v2.VerbType;

public abstract class OaiPmhRequest {
  public abstract VerbType getVerbType();
}
