package no.sikt.nva.oai.pmh.handler.oaipmh;

import org.openarchives.oai.pmh.v2.OAIPMHerrorcodeType;

public abstract class OaiPmhException extends RuntimeException {
  private final OAIPMHerrorcodeType codeType;

  protected OaiPmhException(OAIPMHerrorcodeType codeType, String message) {
    super(message);
    this.codeType = codeType;
  }

  public OAIPMHerrorcodeType getCodeType() {
    return codeType;
  }
}
