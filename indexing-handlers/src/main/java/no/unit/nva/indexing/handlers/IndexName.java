package no.unit.nva.indexing.handlers;

import com.fasterxml.jackson.annotation.JsonValue;

public enum IndexName {
  RESOURCES("resources"),
  TICKETS("tickets"),
  IMPORT_CANDIDATES("import-candidate");

  private final String value;

  IndexName(String value) {
    this.value = value;
  }

  @JsonValue
  public String getValue() {
    return value;
  }
}
