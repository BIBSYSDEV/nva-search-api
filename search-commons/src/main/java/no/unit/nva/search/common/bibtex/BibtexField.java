package no.unit.nva.search.common.bibtex;

import java.util.Comparator;

public record BibtexField(String key, String value) {
  public static final Comparator<BibtexField> BY_KEY = Comparator.comparing(BibtexField::key);

  @Override
  public String toString() {
    return "  %s = {%s}".formatted(key, value);
  }
}
