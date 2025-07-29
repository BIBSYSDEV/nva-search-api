package no.sikt.nva.oai.pmh.handler.oaipmh;

import org.openarchives.oai.pmh.v2.VerbType;

public class IdentifyRequest extends OaiPmhRequest {

  public IdentifyRequest() {
    super();
  }

  @Override
  public VerbType getVerbType() {
    return VerbType.IDENTIFY;
  }
}
