package no.sikt.nva.oai.pmh.handler.oaipmh;

import static java.util.Objects.nonNull;

import org.openarchives.oai.pmh.v2.VerbType;

public class ListRecordsRequest extends OaiPmhRequest {
  private final OaiPmhDateTime from;
  private final OaiPmhDateTime until;
  private final String set;
  private final ResumptionToken resumptionToken;
  private final String metadataPrefix;

  public ListRecordsRequest(
      OaiPmhDateTime from, OaiPmhDateTime until, String set, String metadataPrefix) {
    super();
    this.resumptionToken = null;
    this.from = from;
    this.until = until;
    this.set = set;
    this.metadataPrefix = metadataPrefix;
  }

  public ListRecordsRequest(ResumptionToken resumptionToken) {
    super();
    this.resumptionToken = resumptionToken;
    this.from = null;
    this.until = null;
    this.set = null;
    this.metadataPrefix = null;
  }

  public OaiPmhDateTime getFrom() {
    return nonNull(resumptionToken) ? resumptionToken.originalRequest().getFrom() : from;
  }

  public OaiPmhDateTime getUntil() {
    return nonNull(resumptionToken) ? resumptionToken.originalRequest().getUntil() : until;
  }

  public String getSet() {
    return set;
  }

  public ResumptionToken getResumptionToken() {
    return resumptionToken;
  }

  public String getMetadataPrefix() {
    return nonNull(resumptionToken)
        ? resumptionToken.originalRequest().getMetadataPrefix()
        : metadataPrefix;
  }

  @Override
  public VerbType getVerbType() {
    return VerbType.LIST_RECORDS;
  }
}
