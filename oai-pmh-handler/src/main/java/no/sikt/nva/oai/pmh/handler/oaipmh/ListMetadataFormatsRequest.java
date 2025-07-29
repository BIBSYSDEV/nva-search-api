package no.sikt.nva.oai.pmh.handler.oaipmh;

import java.util.Optional;
import nva.commons.core.JacocoGenerated;
import org.openarchives.oai.pmh.v2.VerbType;

public class ListMetadataFormatsRequest extends OaiPmhRequest {
  private final String identifier;

  public ListMetadataFormatsRequest(final String identifier) {
    super();
    this.identifier = identifier;
  }

  @JacocoGenerated // part of the spec, but we currently have no record-specific formats
  public Optional<String> getIdentifier() {
    return Optional.ofNullable(identifier);
  }

  @Override
  public VerbType getVerbType() {
    return VerbType.LIST_METADATA_FORMATS;
  }
}
