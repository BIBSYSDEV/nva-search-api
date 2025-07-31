package no.sikt.nva.oai.pmh.handler.oaipmh;

import static java.util.Objects.nonNull;

import org.openarchives.oai.pmh.v2.VerbType;

public class ListRecordsRequest extends OaiPmhRequest {
  private final OaiPmhDateTime from;
  private final OaiPmhDateTime until;
  private final SetSpec setSpec;
  private final ResumptionToken resumptionToken;
  private final MetadataPrefix metadataPrefix;

  public ListRecordsRequest(
      OaiPmhDateTime from, OaiPmhDateTime until, SetSpec setSpec, MetadataPrefix metadataPrefix) {
    super();
    this.resumptionToken = null;
    this.from = from;
    this.until = until;
    this.setSpec = setSpec;
    this.metadataPrefix = metadataPrefix;
  }

  public ListRecordsRequest(ResumptionToken resumptionToken) {
    super();
    this.resumptionToken = resumptionToken;
    this.from = null;
    this.until = null;
    this.setSpec = null;
    this.metadataPrefix = null;
  }

  public OaiPmhDateTime getFrom() {
    return nonNull(resumptionToken) ? resumptionToken.originalRequest().getFrom() : from;
  }

  public OaiPmhDateTime getUntil() {
    return nonNull(resumptionToken) ? resumptionToken.originalRequest().getUntil() : until;
  }

  public SetSpec getSetSpec() {
    return nonNull(resumptionToken) ? resumptionToken.originalRequest().getSetSpec() : setSpec;
  }

  public ResumptionToken getResumptionToken() {
    return resumptionToken;
  }

  public MetadataPrefix getMetadataPrefix() {
    return nonNull(resumptionToken)
        ? resumptionToken.originalRequest().getMetadataPrefix()
        : metadataPrefix;
  }

  @Override
  public VerbType getVerbType() {
    return VerbType.LIST_RECORDS;
  }
}
