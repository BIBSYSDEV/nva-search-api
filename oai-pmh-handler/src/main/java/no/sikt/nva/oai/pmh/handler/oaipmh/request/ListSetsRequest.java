package no.sikt.nva.oai.pmh.handler.oaipmh.request;

import org.openarchives.oai.pmh.v2.VerbType;

public class ListSetsRequest extends OaiPmhRequest {

  public ListSetsRequest() {
    super();
  }

  @Override
  public VerbType getVerbType() {
    return VerbType.LIST_SETS;
  }
}
