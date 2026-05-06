package no.unit.nva.search.common.bibtex;

import static no.unit.nva.search.common.bibtex.BibtexField.BY_KEY;

import java.util.Collection;
import java.util.stream.Collectors;

public record BibtexEntry(BibtexConstants type, String key, Collection<BibtexField> fields) {
  @Override
  public String toString() {
    var bibtexFields =
        fields.stream()
            .sorted(BY_KEY)
            .map(BibtexField::toString)
            .collect(Collectors.joining(",\n"));
    return "@%s{%s,\n%s\n}".formatted(type, key, bibtexFields);
  }
}
