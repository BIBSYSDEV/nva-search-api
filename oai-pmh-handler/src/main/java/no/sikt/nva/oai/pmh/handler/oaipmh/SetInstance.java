package no.sikt.nva.oai.pmh.handler.oaipmh;

public record SetInstance(Set set, String value) {

  private static final int TWO = 2;

  public static SetInstance from(String source) {
    var parts = source.split(":");
    if (parts.length == TWO) {
      return new SetInstance(Set.from(parts[0]), parts[1]);
    } else {
      throw new IllegalArgumentException("Invalid Set instance: " + source);
    }
  }
}
