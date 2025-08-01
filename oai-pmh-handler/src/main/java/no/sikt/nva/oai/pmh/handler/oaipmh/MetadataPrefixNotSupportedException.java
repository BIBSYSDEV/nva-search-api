package no.sikt.nva.oai.pmh.handler.oaipmh;

import org.openarchives.oai.pmh.v2.OAIPMHerrorcodeType;

public class MetadataPrefixNotSupportedException extends OaiPmhException {
  public MetadataPrefixNotSupportedException(String message) {
    super(OAIPMHerrorcodeType.CANNOT_DISSEMINATE_FORMAT, message);
  }
}
