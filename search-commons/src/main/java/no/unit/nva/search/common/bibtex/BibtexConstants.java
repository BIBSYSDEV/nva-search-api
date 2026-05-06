package no.unit.nva.search.common.bibtex;

import java.util.Locale;

public enum BibtexConstants {
  ARTICLE,
  BOOK,
  INBOOK,
  INPROCEEDINGS,
  TECHREPORT,
  MASTERSTHESIS,
  PHDTHESIS,
  MISC;

  @Override
  public String toString() {
    return name().toLowerCase(Locale.ROOT);
  }
}
