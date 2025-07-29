package no.sikt.nva.oai.pmh.handler.oaipmh;

import org.openarchives.oai.pmh.v2.OAIPMHerrorcodeType;

public class BadArgumentException extends OaiPmhException {

  public BadArgumentException(String message) {
    super(OAIPMHerrorcodeType.BAD_ARGUMENT, message);
  }
}
