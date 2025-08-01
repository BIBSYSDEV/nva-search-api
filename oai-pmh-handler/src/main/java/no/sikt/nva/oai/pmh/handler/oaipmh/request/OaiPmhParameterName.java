package no.sikt.nva.oai.pmh.handler.oaipmh.request;

public enum OaiPmhParameterName {
  VERB("verb"),
  IDENTIFIER("identifier"),
  RESUMPTION_TOKEN("resumptionToken"),
  METADATA_PREFIX("metadataPrefix"),
  FROM("from"),
  UNTIL("until"),
  SET("set");

  private final String name;

  OaiPmhParameterName(String name) {
    this.name = name;
  }

  public String getName() {
    return name;
  }
}
