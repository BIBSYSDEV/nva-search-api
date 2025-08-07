package no.sikt.nva.oai.pmh.handler.oaipmh.request;

import no.sikt.nva.oai.pmh.handler.oaipmh.MetadataPrefix;
import org.openarchives.oai.pmh.v2.VerbType;

public class GetRecordRequest extends OaiPmhRequest {
  private final String identifier;
  private final MetadataPrefix metadataPrefix;

  public GetRecordRequest(String identifier, MetadataPrefix metadataPrefix) {
    super();
    this.identifier = identifier;
    this.metadataPrefix = metadataPrefix;
  }

  public String getIdentifier() {
    return identifier;
  }

  public MetadataPrefix getMetadataPrefix() {
    return metadataPrefix;
  }

  @Override
  public VerbType getVerbType() {
    return VerbType.GET_RECORD;
  }
}
