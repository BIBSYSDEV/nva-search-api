package no.sikt.nva.oai.pmh.handler.oaipmh;

import java.util.Optional;
import org.openarchives.oai.pmh.v2.VerbType;

public final class OaiPmhRequest {

  private final VerbType verb;
  private final String from;
  private final String until;
  private final String metadataPrefix;
  private final String resumptionToken;

  public static OaiPmhRequest parse(
      String verb, String from, String until, String metadataPrefix, String resumptionToken) {
    return new OaiPmhRequest(toVerbType(verb), from, until, metadataPrefix, resumptionToken);
  }

  private OaiPmhRequest(
      VerbType verb, String from, String until, String metadataPrefix, String resumptionToken) {
    this.verb = verb;
    this.from = from;
    this.until = until;
    this.metadataPrefix = metadataPrefix;
    this.resumptionToken = resumptionToken;
  }

  public Optional<VerbType> getVerb() {
    return Optional.ofNullable(verb);
  }

  public String getFrom() {
    return from;
  }

  public String getUntil() {
    return until;
  }

  public String getMetadataPrefix() {
    return metadataPrefix;
  }

  public String getResumptionToken() {
    return resumptionToken;
  }

  private static VerbType toVerbType(String verb) {
    try {
      return VerbType.fromValue(verb);
    } catch (IllegalArgumentException e) {
      return null;
    }
  }
}
