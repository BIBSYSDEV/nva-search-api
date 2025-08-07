package no.sikt.nva.oai.pmh.handler.oaipmh;

import org.openarchives.oai.pmh.v2.OAIPMHerrorcodeType;

public class IdDoesNotExistException extends OaiPmhException {
  public IdDoesNotExistException() {
    super(OAIPMHerrorcodeType.ID_DOES_NOT_EXIST, "identifier does not exist");
  }
}
