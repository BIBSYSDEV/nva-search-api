package no.sikt.nva.oai.pmh.handler.oaipmh;

import org.openarchives.oai.pmh.v2.OAIPMHerrorcodeType;

public class BadVerbException extends OaiPmhException {

  public BadVerbException(String message) {
    super(OAIPMHerrorcodeType.BAD_VERB, message);
  }
}
