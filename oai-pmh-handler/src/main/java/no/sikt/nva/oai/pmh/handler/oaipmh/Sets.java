package no.sikt.nva.oai.pmh.handler.oaipmh;

public enum Sets {
  PUBLICATION_INSTANCE_TYPE("PublicationInstanceType");

  private static final String COLON = ":";

  private final String value;

  Sets(String value) {
    this.value = value;
  }

  public String getValue() {
    return value;
  }

  public String getSpec(String type) {
    return getValue() + COLON + type;
  }
}
